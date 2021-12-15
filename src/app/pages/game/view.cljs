(ns app.pages.game.view
  (:require
   [zframes.pages :as pages]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [re-dnd.events] ;; make sure the events are registered and inited properly.
   [re-dnd.views :as dndv] ;;make sure the internal components are registered.
   [re-dnd.subs] ;; make sure subs are registered
   [app.pages.game.model :as model]))

(defmethod dndv/dropped-widget
 :my-drop-marker
 [{:keys [type id]}]
  [:div.my-drop-marker "Some visual showing when dragged object is hovering over drop zone."])
(defmethod dndv/dropped-widget
 :bluebox
 [{:keys [type id]}]
  [:div.my-drop-marker "Some visual showing when dragged object is hovering over drop zone."])
(defmethod dndv/dropped-widget
 :redbox
 [{:keys [type id]}]
  [:div.my-drop-marker "Some visual showing when dragged object is hovering over drop zone."])


(defn my-drop-zone []
  [dndv/drop-zone :drop-zone-1 ;;:drop-zone-1 is a unique identifier
   [:div {:style {:border "1px solid red" :height "100px"}}
    "A custom body for the drop-zone (optional)"]])

(defn my-draggable []
  [dndv/draggable :draggable-1
   [:span "i'm draggable."]])

(defn my-panel []
  (let [;;this state is necesary to determine if we need to show the drag-box
        drag-box-state (rf/subscribe [:dnd/drag-box])]
    ;;It's best not to put this here, but for conciseness it is done here.
    ;;It should be called when loading the page with the dnd functionality.
    ;;It prepares the app-db with some initial state.
    (rf/dispatch [:dnd/initialize-drop-zone
                  :drop-zone-1 ;;note, this key is the same as in my-drop-zone above.
                  {:drop-dispatch [:my-drop-dispatch] ;;re-frame event handler that will be called when draggable is dropped on a drop-zone
                   :drop-marker :my-drop-marker ;;multi-method dispatch-value for dnd/dropped-widget
                  }])
    (fn []
      [:div
        (when @drag-box-state [dndv/drag-box])

        [my-draggable]

        [my-drop-zone]])))

(def last-id (r/atom 0)) ;;or use ie. (str (random-uuid))

(rf/reg-event-fx
 :my-drop-dispatch
 (fn [{db :db}
      [_
       ;; the callback contains two vectors, of the source and of the target.
       ;; Note the source-drop-zone-id, it's possible the dropped element actually comes from
       ;; a drop-zone (ie. re-ordering within the drop-zone). In the example above, we have an external
       ;; draggable, in which case source-drop-zone-id would be nil.
       [source-drop-zone-id source-element-id]
       [drop-zone-id dropped-element-id dropped-position]]] ;;position = index in the list of dropped elements
   (swap! last-id inc)
   {:db       db
    :dispatch
    ;;if the source drop-zone and target drop-zone is the same, it means we need to re-order the items
    ;; (at least, in this example we want that, but what you want is completely up to you :-))
    (if (= source-drop-zone-id drop-zone-id)
      ;;built-in dispatch for re-ordering elements in a drop-zone
      [:dnd/move-drop-zone-element drop-zone-id source-element-id dropped-position]

      ;;Built-in dispatch for adding a drop-zone-element ('dropped-element') in a drop-zone.
      ;;Our current logic is to just add a new entry to the drop-zone.
      ;;Your requirement might be different.
      [:dnd/add-drop-zone-element
       drop-zone-id
       {:id   (keyword (str (name source-element-id) "-dropped-" @last-id))
        ;;The type key is the dispatch-value of the dndv/dropped-widget multi-method.
        ;;thus, by means of multi-methods we can create any component we'd like.
        :type (if (odd? @last-id )
                :bluebox
                :redbox)}
       dropped-position])}))


(defn drag-golden [pt]
  [:div.rpgui-container.framed-golden-2.pos-initial.rpgui-cursor-grab-open.drag.p-8.pt
   [:h3 (get-in pt [:name 0 :given 0])]
   [:div.flex.pt-10
    [:img.pt-monitor {:src "./img/monitor.png"}]
    [:div.grow-1
     [:div [:span.pt-hp [:img.pt-icn {:src "./img/heart.png"}]
            (:health pt)  "/10"]]
     [:div [:span.pt-mn [:img.pt-icn {:src "./img/coin_gold.png"}]
            (:balance pt) "/20"]]]]
   [:div.pt-stats
    [:div [:span [:img.pt-icn {:src "./img/temp.png"}]       "5/5 temperature"]]
    [:div [:span [:img.pt-icn {:src "./img/potion_red.png"}] "3/5 pressure"]]
    [:div [:span [:img.pt-icn {:src "./img/sugar.png"}]      "2/5 sugar"]]
    [:div [:span [:img.pt-icn {:src "./img/orc_green.png"}]  "2/5 bacteria"]]
    [:div [:span [:img.pt-icn {:src "./img/coin_gold.png"}]  "4/5 diarea"]]]])

(defn plusify [t] (if (> t 0 ) (str "+" t) t))

(defn drag [m]
   [:div.rpgui-container.framed.pos-initial.rpgui-cursor-grab-open.drag.rpgui-draggable
    [:h3 (:name m)]
    [:div.flex
     [:div.rpgui-icon.empty-slot.tabl-ico
      [:img.med-img {:src (:img m)}]]
     [:div.grow-1

      (when-let [t (get-in m [:effects :temperature])]
        [:div [:span [:img.pt-icn {:src "./img/temp.png"}] (plusify t)]])
      (when-let [t (get-in m [:effects :pressure])]
        [:div [:span [:img.pt-icn {:src "./img/potion_red.png"}] (plusify t)]])
      (when-let [t (get-in m [:effects :sugar])]
        [:div [:span [:img.pt-icn {:src "./img/sugar.png"}] (plusify t)]])
      (when-let [t (get-in m [:effects :bacteria])]
        [:div [:span [:img.pt-icn {:src "./img/orc_green.png"}] (plusify t)]])
      (when-let [t (get-in m [:effects :diarea])]
        [:div [:span [:img.pt-icn {:src "./img/coin_gold.png"}] (plusify t)]])
      ]]])

(defn aidbox [med]
   [:div.rpgui-container.framed-golden.pos-initial.aidbox.flex
    (into [:<>]
          (for [i med]  ^{:key (:id i)}
            (drag i)))])

(pages/reg-subs-page
 model/index-page
 (fn [{dv :d pts :pts medics :aidbox :as  page} _]
   [:div.game.rpgui-container.framed.relative {:style {:padding "0"}}

    [my-panel]


    [:div.flex
     [:div.top-left-bordur]
     [:div.top-bordur.grow-1]]
    [:div.flex
     [:div.left-bordur]
     [:div.grow-1
      [:div.top-wall]
      [:div.top-90
       (when (> (count pts) 1)
         [:div.flex
          [drag-golden (nth pts 0)]
          [drag-golden (nth pts 1)]
          [drag-golden (nth pts 2)]
          ])
       [:img.blood {:src "./img/blood.png"}]
       [:img.patient {:src "./img/patient.png"}]
       [:img.koika {:src "./img/koika.png"}]
       [:img.tumba {:src "./img/tumba.png"}]
       [:img.wall {:src "./img/wall.png"}]
       [:img.patient {:src "./img/patient.png"}]
       [:img.koika {:src "./img/koika.png"}]
       [:img.tumba {:src "./img/tumba.png"}]
       [:img.wall {:src "./img/wall.png"}]
       [:img.patient {:src "./img/patient.png"}]
       [:img.koika {:src "./img/koika.png"}]]
      [aidbox medics]
      [:div
       [:div.rpgui-progress.blue {:data-rpguitype "progress"}
        [:div.rpgui-progress-track
         [:div.rpgui-progress-fill.blue {:style {:width "20%"}}]]
        [:div.rpgui-progress-left-edge]
        [:div.rpgui-progress-right-edge]
        ]]
      ]]

    [:br]]))
