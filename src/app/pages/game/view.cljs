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
 [_]
  [:span.my-drop-marker "Drop marker"])

(defmethod dndv/dropped-widget
  :dropped-box
  [{:keys [type id]}]
  #_[:span.my-drop-marker ])

(def last-id (r/atom 0)) ;;or use ie. (str (random-uuid))

(rf/reg-event-fx
 :my-drop-dispatch
 (fn [{db :db}
      [_
       [source-drop-zone-id drug-id]
       [drop-zone-id dropped-element-id dropped-position]]]
   ;;position = index in the list of dropped elements
   (prn "=================" "zone" drop-zone-id  "element" drug-id)
   (let [drug (get-in db [:aidbox (name drug-id)])
         pocik (get-in db [:patients (name drop-zone-id)])]
       (swap! last-id inc)
       (prn "Apply drug:" drug)
       (prn "For pocik :" pocik)
     {:db       db
      :dispatch  [::model/apply-drug pocik drug]})))

(defn stat-color [m d]
  (if d
    "red"
    (if (> m 2) "green" (if (> m 1) "yellow" "red"))))

(defn drag-golden [pt obs]
  (let [o (group-by #(get-in % [:code :coding 0 :code]) obs)
        death? (get-in pt [:deceased :boolean])
        d death?]
    [:div.rpgui-container.pos-initial.drag.p-8.pt
     {:class (if death? "framed" "framed-golden")}
     [:h3 (get-in pt [:name 0 :given 0])]
     [:div.flex.pt-10
      [:img.pt-monitor (if death?
                         {:src "./img/red-monitor.png"}
                         {:src "./img/monitor.png"})]
      [:div.grow-1
       [:div [:span.pt-hp [:img.pt-icn {:src "./img/heart.png"}]
              (:health pt)  "/10"]]
       [:div [:span.pt-mn [:img.pt-icn {:src "./img/coin_gold.png"}]
              (:balance pt) "/20"]]]]
     [:div.pt-stats
      (let [m (get-in (get o "temperature") [0 :value :Quantity :value])]
        [:div [:span.stat-row {:class (stat-color m d)} [:img.pt-icn {:src "./img/thermometer.png"}]   m "/5 temperature"]])
      (let [m (get-in (get o "pressure")    [0 :value :Quantity :value])]
        [:div [:span.stat-row {:class (stat-color m d)} [:img.pt-icn {:src "./img/tonometer.png"}]     m "/5 pressure"]])
      (let [m (get-in (get o "sugar")       [0 :value :Quantity :value])]
        [:div [:span.stat-row {:class (stat-color m d)} [:img.pt-icn {:src "./img/sugar.png"}]         m "/5 sugar"]])
      (let [m (get-in (get o "bacteria")    [0 :value :Quantity :value])]
        [:div [:span.stat-row {:class (stat-color m d)} [:img.pt-icn {:src "./img/orc_green.png"}]     m "/5 bacteria"]])
      (let [m (get-in (get o "diarrhea")    [0 :value :Quantity :value])]
        [:div [:span.stat-row {:class (stat-color m d)} [:img.pt-icn {:src "./img/diarrhea.png"}]      m "/5 diarrhea"]])]]))

(defn plusify [t] (if (> t 0 ) (str "+" t) t))

(defn drug [id m]
  [:div.ib
   [dndv/draggable (keyword id)
    [:div.rpgui-container.framed.pos-initial.rpgui-cursor-grab-open.drag.rpgui-draggable
     [:h3 (:name m)]
     [:div.flex
      [:div.rpgui-icon.empty-slot.tabl-ico
       [:img.med-img {:src (:image m)}]]
      [:div.grow-1
       [:span "AP: " (:action-point m)]
       (when-let [t (get-in m [:price])]
         [:div [:span [:img.pt-icn {:src "./img/coin_gold.png"}]  t]])
       (when-let [t (get-in m [:effects :temperature])]
         [:div [:span [:img.pt-icn {:src "./img/thermometer.png"}] (plusify t)]])
       (when-let [t (get-in m [:effects :pressure])]
         [:div [:span [:img.pt-icn {:src "./img/tonometer.png"}] (plusify t)]])
       (when-let [t (get-in m [:effects :sugar])]
         [:div [:span [:img.pt-icn {:src "./img/sugar.png"}] (plusify t)]])
       (when-let [t (get-in m [:effects :bacteria])]
         [:div [:span [:img.pt-icn {:src "./img/orc_green.png"}] (plusify t)]])
       (when-let [t (get-in m [:effects :diarrhea])]
         [:div [:span [:img.pt-icn {:src "./img/diarrhea.png"}] (plusify t)]])


       ]]]]])

(defonce state (r/atom {:selected :temperature}))

(defn aidbox [_ _]
  (let [set-fltr #(swap! state assoc :selected %)]
    (fn [med {:keys [total current]:as ap}]
      [:div.rpgui-container.framed-golden.pos-initial
       [:div.tab
        [:img.pt-icn.rpgui-cursor-point {:on-click #(set-fltr :temperature)
                      :src "./img/thermometer.png" :class (when (= :temperature (:selected @state)) "active")}]
        [:img.pt-icn.rpgui-cursor-point {:on-click #(set-fltr :pressure)
                      :src "./img/tonometer.png"   :class (when (= :pressure    (:selected @state)) "active")}]
        [:img.pt-icn.rpgui-cursor-point {:on-click #(set-fltr :sugar)
                      :src "./img/sugar.png"       :class (when (= :sugar       (:selected @state)) "active")}]
        [:img.pt-icn.rpgui-cursor-point {:on-click #(set-fltr :bacteria)
                      :src "./img/orc_green.png"   :class (when (= :bacteria    (:selected @state)) "active")}]
        [:img.pt-icn.rpgui-cursor-point {:on-click #(set-fltr :diarrhea)
                      :src "./img/diarrhea.png"    :class (when (= :diarrhea    (:selected @state)) "active")}]]
       [:hr]
       [:div.apps
        (into [:<>]
              (for [a (repeat current "x")]
                [:img  {:width "20px" :src "./dist/img/radio-on.png"}]))
        (into [:<>]
              (for [a (repeat (- total current) "x")]
                [:img  {:width "20px" :src "./dist/img/radio-off.png"}]))

        (into [:<>]
              (for [a (repeat (- 10 total) "x")]
                [:img.dsbl {:width "20px" :src "./dist/img/radio-off.png"}]))]

       [:hr]
       [:div.aidbox
        (let [mmeds (reduce-kv
                     (fn [acc k v]
                       (if (> (get-in v [:effects (:selected @state)]) 0)
                         (assoc acc k v)
                         acc))
                     {} med)
              mmeds  (sort-by  (juxt
                                #(get-in (val %) [:action-point])
                                #(- (get-in (val %) [:effects (:selected @state)]))
                                )  mmeds)]
          (into
           [:<>]
           (for [[id res] mmeds]  ^{:key id}
             (drug id res))))]])))

(defn koika-1 [patient]
  [:div
   [:img.tumba   {:src "./img/tumba.png"}]
   [:img.patient {:src (str "./img/" (or (:avatar patient) "patient.png"))}]
   [:img.koika   {:src "./img/koika.png"}]
   [:img.wall    {:src "./img/wall.png"}]

   ])

(defn koika-2 [patient]
  [:div
   [:img.blood   {:src "./img/blood.png"}]
   [:img.patient {:src (str "./img/" (or (:avatar patient) "patient.png"))}]
   [:img.koika   {:src "./img/koika.png"}]
   [:img.tumba   {:src "./img/tumba.png"}]
   [:img.wall    {:src "./img/wall.png"}]
   ])

(defn koika-3 [patient]
  (prn patient)
  [:div
   [:img.patient {:src (str "./img/" (or (:avatar patient) "patient.png"))}]
   [:img.koika   {:src "./img/koika.png"}]
   [:img.tumba   {:src "./img/tumba.png"}]
   ])

(defn koika [idx patients]
  (let [vec-patients (vec patients)
        k (case idx
            0 koika-1
            1 koika-2
            koika-3)]
    [k (val (nth vec-patients idx))]))

(pages/reg-subs-page
 model/index-page
 (fn [{dv :d pts :pts medics :aidbox ap :ap :as  page} _]
   (let [drag-box-state (rf/subscribe [:dnd/drag-box])
         patients (rf/subscribe [::model/patients])]
     (fn [{dv :d pts :pts medics :aidbox obs :obs :as page} _]

       [:div.game.rpgui-container.framed.relative
        (when @drag-box-state [dndv/drag-box])

        [:div.fsgrid
         [:div#g-patients
          [:div.flex
           (into [:<>]
                 (for [[idx [k v]] (map-indexed vector pts)] ^{:key k}
                   [dndv/drop-zone (keyword k)
                    [:div
                     [drag-golden v (get obs k)]
                     [koika idx @patients]]]))]]

         [:div#g-aidbox
          [aidbox medics ap]]

         [:div#g-progress
          [:div.flex.ac
           [:div.grow-1.mr-3
            [:div
             [:div.rpgui-progress.blue {:data-rpguitype "progress"}
              [:div.rpgui-progress-track
               [:div.rpgui-progress-fill.blue
                {:style {:width (str (/ (* 100 (or (:game-step page) 1) ) 10) "%")}}]]
              [:div.rpgui-progress-left-edge]
              [:div.rpgui-progress-right-edge]]]]

           [:button.rpgui-button.golden
            {:style {:padding-top "0px"}
             :on-click #(rf/dispatch [::model/next-step])}
            [:p {:style {:padding-top "5px"}} "Далее " (:game-step page) "/10"]]]]]]))))
