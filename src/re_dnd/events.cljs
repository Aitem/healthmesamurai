(ns re-dnd.events
  (:require [re-frame.core :as re-frame]
            [re-frame.core :as rf]
            [reagent.core :as reagent]
            [taoensso.timbre :as timbre
             :refer-macros (log  trace  debug  info  warn  error  fatal  report
                                 logf tracef debugf infof warnf errorf fatalf reportf
                                 spy get-env log-env)]
            [vimsical.re-frame.cofx.inject :as inject]))

(defonce reg-event-listeners
  (memoize ; evaluate fn only once
   (fn []
     (.addEventListener js/document.body "mousemove" #(re-frame/dispatch [:dnd/mouse-moves
                                                                          (+ (.-clientX %)
                                                                             (.-scrollX js/window))
                                                                          (+
                                                                           (.-clientY %)
                                                                           (.-scrollY js/window))]))
     (.addEventListener js/document.body "mousedown" #(rf/dispatch [:dnd/set-mouse-button-status true]))
     (.addEventListener js/document.body "mouseup" #(rf/dispatch [:dnd/set-mouse-button-status false])))))

(defn flip-args
  [f x y]
  (f y x))

(defn bounding-rect
  [e]
  (if (nil? e)
    nil
    (let [rect (.getBoundingClientRect e)
          pos  {:top    (.-top rect)
                :left   (.-left rect)
                :bottom (.-bottom rect)
                :right  (.-right rect)}

          pos' (-> pos
                  (update :top    + (.-scrollY js/window))
                  (update :right  + (.-scrollX js/window))
                  (update :bottom + (.-scrollY js/window))
                  (update :left   + (.-scrollX js/window)))]

      ;;(debug pos)
      ;;(debug pos')
      pos')))


(defn collides?
  [{r1 :right l1 :left t1 :top b1 :bottom}
   {r2 :right l2 :left t2 :top b2 :bottom}]
  (not (or (< r1 l2) (> l1 r2) (< b1 t2) (> t1 b2))))

(defn translate
  "Moves a boundingClientRect with x and y pixels"
  [sub x y]
  (-> sub
      (update :left + x)
      (update :right - x)
      (update :top + y)
      (update :bottom - y)))

(defn set-all-draggables-to-idle
  [db]
  (-> db
      (update-in [:dnd/state :drop-zones]
              (fn [o]
                (into {}
                      (map (fn [[k v]]
                             [k
                              (map (fn [v']
                                     (-> v'
                                         (assoc :status :idle)
                                         (dissoc :position)))
                                   v)])
                           o))))
      (update-in [:dnd/state :draggables]
              (fn [o]
                (into {}
                      (map (fn [[k v]]
                             [k (assoc v :status :idle)])
                           o))))))

(defn insert-at-pos
  [pos hmap e]
  (let [[h t] (split-at pos hmap)]
    (concat h [e] t)))

(defn move-element-in-list
  [m k new-pos]
  (let [comparator (comp #{k} :id)
        e          (->> m
                        (filter comparator)
                        first)
        [h t]      (split-at new-pos m)]
    (debug (map :id m) k (:id e) new-pos)
    (concat (remove comparator h) [e] (remove comparator t))))

(defn positions
  [pred coll]
  (keep-indexed (fn [idx x]
                  (when (pred x)
                    idx))
                coll))

(defn index-of
  [search coll]
  (first (positions #{search} coll)))

(defn get-position-of-element
  [db dz-id elt-id]
  (let [elts (get-in db [:dnd/state :drop-zones dz-id])]
    (index-of elt-id (mapv :id elts))))

(rf/reg-event-fx
 :dnd/move-up
 (fn [{db :db} [_ dz-id elt-id]]
   (let [options (get-in db [:dnd/state :drop-zone-options dz-id])
         idx     (get-position-of-element db dz-id elt-id)
         disp    (if (:drop-dispatch options)
                   (into (:drop-dispatch options)
                         [[dz-id elt-id]
                          [dz-id elt-id (dec idx)]])
                   [:dnd/move-drop-zone-element dz-id elt-id (dec idx)])]
     (if (pos? idx)
       {:db       db
        :dispatch disp}
       ;;else
       {:db db}))))

(rf/reg-event-fx
 :dnd/move-down
 (fn [{db :db} [_ dz-id elt-id]]
   (let [options  (get-in db [:dnd/state :drop-zone-options dz-id])
         num-elts (count (get-in db [:dnd/state :drop-zones dz-id]))
         idx      (get-position-of-element db dz-id elt-id)
         disp     (if (:drop-dispatch options)
                    (into (:drop-dispatch options)
                          [[dz-id elt-id]
                           [dz-id elt-id (inc (inc idx))]])
                    [:dnd/move-drop-zone-element dz-id elt-id (inc (inc idx))])]
     {:db       db
      :dispatch disp})))


(re-frame/reg-event-db
 :dnd/delete-drop-zone-element
 (fn [db [_ dz-id elt-id]]
   (update-in db [:dnd/state :drop-zones dz-id]
              #(remove (fn [{id :id}]
                         (= %2 id))
                       %1)
              elt-id)))

(re-frame/reg-event-db
 :dnd/move-drop-zone-element
 (fn [db [_ dz-id e-id new-pos]]
   (update-in db [:dnd/state :drop-zones dz-id] move-element-in-list e-id (or new-pos 0))))

(re-frame/reg-event-db
 :dnd/add-drop-zone-element
 (fn [db [_ drop-zone-id {:keys [id type] :as elt} dropped-position]]
   (assert id "Please set a :id key in the second parameter of options.")
   (when-not type
     (warn "Please set a :type key in the second parameter of options"))
   (if-not dropped-position ;;append
     (update-in db [:dnd/state :drop-zones drop-zone-id]
                (let [elts             (get-in db [:dnd/state :drop-zones drop-zone-id])
                      dropped-position (count elts)]
                  (partial insert-at-pos dropped-position))
                elt)
     (update-in db
                [:dnd/state :drop-zones drop-zone-id]
                (partial insert-at-pos dropped-position)
                elt))))

(re-frame/reg-event-fx
 :dnd/initialize-drop-zone
 (fn [{db :db} [_ id opts initial-elements]]
   (reg-event-listeners)
   (let [initial-disps (map (fn [elt]
                              [:dnd/add-drop-zone-element id elt])
                            initial-elements)]
     {:db
      (-> db
          (assoc-in [:dnd/state :drop-zone-options id] opts)
          (assoc-in [:dnd/state :drop-zones id] []))
      :dispatch-n initial-disps})))

(defn find-first-dragging-element
  [db]
  (let [d (->> (get-in db [:dnd/state :draggables])
               (filter (fn [[k v]] (= (:status v) :dragging)))
               ;;gets the key
               ffirst)
        d' (->> (get-in db [:dnd/state :drop-zones])
                (map (fn [[dz-id dz]]
                       [dz-id (->> dz
                                   (filter
                                    (comp (partial = :dragging) :status))
                                   first
                                   :id)]))
                (remove (comp nil? second))
                first)]
    (if d
      [nil d]
      d')))

(re-frame/reg-event-fx
 :dnd/set-mouse-button-status
 (fn [{db :db} [_ down?]]
   ;;when not down?, check first dragging id, and handle a drop
   ;; through a re-dispatch for cleanliness
   (let [[drop-zone-id draggable-id] (find-first-dragging-element db)]
     (cond->
         {:db (assoc db :mouse-button down?)}
       (and (not down?) draggable-id)
       (assoc :dispatch [:dnd/end-drag draggable-id drop-zone-id])))))

(re-frame/reg-event-fx
 :dnd/mouse-moves
 (fn [{db :db} [_ x y]]
;;   (debug "mouse-moves:" x y)
   (let [db' (assoc-in db [:dnd/state :mouse-position] {:x x :y y})]
     (if (:mouse-button db)
       (let [[drop-zone-id draggable-id] (find-first-dragging-element db)]
         (if draggable-id
           {:db       db'
            :dispatch [:dnd/drag-move draggable-id drop-zone-id x y]}
           {:db db'}))
       {:db db'}))))

(defn clear-selection
  []
  (let [sel (.-selection js/document)]
    (if (and sel (.hasOwnProperty sel "empty"))
      (.empty sel)
      ;;else
      (do
        (when (.-getSelection js/window)
          (.removeAllRanges (.getSelection js/window)))
        (if-let [ae (.-activeElement js/document)]
          (let [tag-name (-> ae .-nodeName .toLowerCase)]
            (if (or
                 (and
                  (= "text" (.-type ae))
                  (= "input" tag-name))
                 (= "textarea" tag-name))
              (set! (.-selectionStart ae) (.-selectionEnd ae)))))))))

(defn update-dz-elt
  [db drop-zone-id elt-id f]
  (update-in db [:dnd/state :drop-zones drop-zone-id]
             (fn [elts id']
               (map (fn [e]
                      (if (= id' (:id e))
                        (f e)
                        e))
                    elts))
             elt-id))

(re-frame/reg-event-db
 :dnd/drag-body
 (fn [db [_ id body]]
   (assoc-in db [:dnd/state :draggables id :body] body)))

(re-frame/reg-event-db
 :dnd/drag-move
 (fn [db [_ id drop-zone-id x y ]]
   ;;(debug "drag-move" id x y)
   (assert id)
   (when id
     (clear-selection))
   (let [body (get-in db [:dnd/state :draggables id :body])
         pos     {:x (- (- x (.-scrollX js/window)) 20)
                  :y (- (- y (.-scrollY js/window)) 20)}
         change (fn [prev]
                  (let [offset (or (:offset prev)
                                   {:x (- (:x pos)(:x prev))
                                    :y (- (:y pos)(:y prev))})]
                    (-> (merge prev pos)
                        (update :x - (:x offset))
                        (update :y - (:y offset))
                        (assoc :body (when body body))
                        (assoc :offset offset))))]
     (if drop-zone-id
       (update-dz-elt db drop-zone-id id (fn [e]
                                           (update e :position change)))
       (update-in db [:dnd/state :draggables id :position] change)))))

(re-frame/reg-event-db
 :dnd/hover
 (fn  [db [_ id drop-zone-id hover-in?]]
   (if (:mouse-button db)
     db
     (if drop-zone-id
       (update-dz-elt db drop-zone-id id
                      (fn [e]
                        (assoc e :status (if hover-in? :hover :idle))))
       ;;else just a normal draggable
       (assoc-in db [:dnd/state :draggables id :status] (if hover-in? :hover nil))))))

(re-frame/reg-event-db
 :dnd/start-drag
 (fn  [db [_ id drop-zone-id x y w h]]
   (let []
     (debug (str "start-drag " drop-zone-id "," id ", x: " x ", y: " y ", w: " w ", h: " h))
     (let [pos {:x      (- x (.-scrollX js/window))
                :y      (- y (.-scrollY js/window)) ;;discount for scroll pos
                :width  w
                :height h}]
       (debug (:y pos) y (.-scrollY js/window))
       (if drop-zone-id

         (-> db
             (update-dz-elt drop-zone-id id
                            (fn [e]
                              (assoc e
                                     :status :dragging
                                     :position pos))))
         ;;else just a normal draggable
         (-> db
             (assoc-in [:dnd/state :draggables id :status] :dragging)
             (assoc-in [:dnd/state :draggables id :position] pos)))))))


(re-frame/reg-event-fx
 :dnd/end-drag
 [(re-frame/inject-cofx ::inject/sub [:dnd/get-colliding-drop-zone-and-index])]
 (fn  [{db                    :db
        drop-zones-being-hit? :dnd/get-colliding-drop-zone-and-index}
       [_ source-draggable-id source-drop-zone-id]]
   (debug drop-zones-being-hit?)
   (let [disps (for [[drop-zone-id [[dropped-element-id index]]] drop-zones-being-hit?]
                 (let [options                  (get-in db [:dnd/state :drop-zone-options drop-zone-id])
                       _                        (debug options)
                       drag-target-hit-dispatch (if (:drop-dispatch options)
                                                  (into (:drop-dispatch options)
                                                        [[source-drop-zone-id source-draggable-id]
                                                         [drop-zone-id dropped-element-id index]])
                                                  (do
                                                    (warn "No options found for drop-zone-id: " drop-zone-id ", make sure it is properly initialized. Ignoring")
                                                    nil))

                       ]
                   drag-target-hit-dispatch))
         disps (remove nil? disps)]
     (debug "dispatches: " disps)
     {:db         (set-all-draggables-to-idle db)
      :dispatch-n (vec disps)})))

(re-frame/reg-event-db
 :dnd/reorder-drop
 (fn [db [_ drop-zone-id dropped-element-id]]
   (let [drag-box    (bounding-rect (.getElementById js/document "drag-box"))
         drop-zone   (bounding-rect (.getElementById js/document (str "drop-zone-" drop-zone-id)))]
     (cond
       (or
        (nil? drop-zone)
        (nil? drag-box))
       (do
         (debug "No dragbox / dropzone")
         db)

       (collides? drag-box drop-zone)
       (do (debug "Colliding!")
           db) ;; TODO fix this

       :otherwise ;;no-op
       (do
         (debug "No collide")
         db)))))
