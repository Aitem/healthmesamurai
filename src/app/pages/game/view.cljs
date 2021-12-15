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


(defn drag-golden [pt obs]
  (let [o (group-by #(get-in % [:code :coding 0 :code]) obs)]
    (prn "->> " o)
    [:div.rpgui-container.framed-golden.pos-initial.rpgui-cursor-grab-open.drag.p-8.pt
     [:h3 (get-in pt [:name 0 :given 0])]
     [:div.flex.pt-10
      [:img.pt-monitor {:src "./img/monitor.png"}]
      [:div.grow-1
       [:div [:span.pt-hp [:img.pt-icn {:src "./img/heart.png"}]
              (:health pt)  "/10"]]
       [:div [:span.pt-mn [:img.pt-icn {:src "./img/coin_gold.png"}]
              (:balance pt) "/20"]]]]
     [:div.pt-stats
      [:div [:span [:img.pt-icn {:src "./img/thermometer.png"}]   (get-in (get o "temperature") [0 :value :Quantity :value]) "/5 temperature"]]
      [:div [:span [:img.pt-icn {:src "./img/tonometer.png"}]     (get-in (get o "pressure")    [0 :value :Quantity :value]) "/5 pressure"]]
      [:div [:span [:img.pt-icn {:src "./img/sugar.png"}]         (get-in (get o "sugar")       [0 :value :Quantity :value]) "/5 sugar"]]
      [:div [:span [:img.pt-icn {:src "./img/orc_green.png"}]     (get-in (get o "bacteria")    [0 :value :Quantity :value]) "/5 bacteria"]]
      [:div [:span [:img.pt-icn {:src "./img/diarrhea.png"}]      (get-in (get o "diarrhea")    [0 :value :Quantity :value]) "/5 diarea"]]]]))

(defn plusify [t] (if (> t 0 ) (str "+" t) t))

(defn drug [id m]
  [dndv/draggable (keyword id)
   [:div.rpgui-container.framed.pos-initial.rpgui-cursor-grab-open.drag.rpgui-draggable
    [:h3 (:name m)]
    [:div.flex
     [:div.rpgui-icon.empty-slot.tabl-ico
      [:img.med-img {:src (:img m)}]]
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
      (when-let [t (get-in m [:effects :diarea])]
        [:div [:span [:img.pt-icn {:src "./img/diarrhea.png"}] (plusify t)]])
      

      ]]]])

(defn aidbox [med]
   [:div.rpgui-container.framed-golden.pos-initial.aidbox.flex
    (into [:<>]
          (for [[id res] med]  ^{:key id}
            (drug id res)))])

(pages/reg-subs-page
 model/index-page
 (fn [{dv :d pts :pts medics :aidbox :as  page} _]
   (let [drag-box-state (rf/subscribe [:dnd/drag-box])]
     (fn [{dv :d pts :pts medics :aidbox obs :obs :as page} _]

       [:div.game.rpgui-container.framed.relative {:style {:padding "0"}}
       (when @drag-box-state [dndv/drag-box])

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

        [:br]]))))
