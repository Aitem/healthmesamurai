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
    [:div.rpgui-container.pos-initial.rpgui-cursor-grab-open.drag.p-8.pt
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
       (when-let [t (get-in m [:price])]
         [:div [:span [:img.pt-icn {:src "./img/coin_gold.png"}] "-" t]])
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

(defn aidbox [med]
   [:div.rpgui-container.framed-golden.pos-initial.aidbox
    (into [:<>]
          (for [[id res] med]  ^{:key id}
            (drug id res)))])

(pages/reg-subs-page
 model/index-page
 (fn [{dv :d pts :pts medics :aidbox :as  page} _]
   (let [drag-box-state (rf/subscribe [:dnd/drag-box])]
     (fn [{dv :d pts :pts medics :aidbox obs :obs :as page} _]
       (when @drag-box-state [dndv/drag-box])

       [:div.game.rpgui-container.framed.relative

        [:div.fsgrid
         [:div#g-patients
          [:div.flex
           (into [:<>]
                 (for [[k v] pts] ^{:key k}
                   [dndv/drop-zone (keyword k) [drag-golden v (get obs k)]]))]]


         [:div#g-aidbox [aidbox medics]]

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
            [:p {:style {:padding-top "5px"}} "Далее " (:game-step page) "/10"]]]]]

        #_[:div.flex
         [:div.top-left-bordur]
         [:div.top-bordur.grow-1]]
        #_[:div.flex
         [:div.left-bordur]
         [:div.grow-1
          [:div.flex
           [:div.top-wall]
           [:div.top-90
            (when (> (count pts) 1)
              [:div.flex
               (into [:<>]
                     (for [[k v] pts] ^{:key k}
                       [dndv/drop-zone (keyword k) [drag-golden v (get obs k)]]))])
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
            [:img.koika {:src "./img/koika.png"}]
            ]
           [aidbox medics]]


          ]]

        [:br]]))))
