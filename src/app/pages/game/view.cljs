(ns app.pages.game.view
  (:require [zframes.pages :as pages]
            [app.pages.game.model :as model]))

(defn drag-golden []
  [:div.rpgui-container.framed-golden-2.pos-initial.rpgui-cursor-grab-open.drag
   [:h3 "Николас"]
   [:div.flex
    [:div.rpgui-icon.empty-slot.tabl-ico]
    [:div.grow-1
     [:div [:span "Temp +1"]]
     [:div [:span "Pressure +1"]]
     [:div [:span "Sugar +3"]]
     [:div [:span "Diarrea +4"]]
     [:div [:span "Bacteria +1"]]
     ]]]
  )

(defn drag []
  [:div.rpgui-container.framed.pos-initial.rpgui-cursor-grab-open.drag
   [:h3 "Aspirinus"]
   [:div.flex
    [:div.rpgui-icon.empty-slot.tabl-ico]
    [:div.grow-1
     [:div [:span "Hp +1"]]
     [:div [:span "Hp +1"]]
     [:div [:span "Hp +1"]]]]]
  )

(defn aidbox []
  [:div.rpgui-container.framed-golden.pos-initial.aidbox.flex
   [drag] [drag]
   [drag] [drag]
   [drag] [drag]
   ])

(pages/reg-subs-page
 model/index-page
 (fn [{dv :d pts :pts :as  page} _]
   [:div.game.rpgui-container.framed.relative {:style {:padding "0"}}
    [:div.flex
     [:div.top-left-bordur]
     [:div.top-bordur.grow-1]]
    [:div.flex
     [:div.left-bordur]
     [:div.grow-1
      [:div.top-wall]
      [:div.top-90
       [:div.aidbox-grid [drag-golden] [drag-golden] [drag-golden]]
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
      [aidbox]
      [:div
       [:div.rpgui-progress.blue {:data-rpguitype "progress"}
        [:div.rpgui-progress-track
         [:div.rpgui-progress-fill.blue {:style {:width "20%"}}]]
        [:div.rpgui-progress-left-edge]
        [:div.rpgui-progress-right-edge]
        ]]
      ]]

    [:br]]))
