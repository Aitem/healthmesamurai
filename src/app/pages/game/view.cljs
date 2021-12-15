(ns app.pages.game.view
  (:require [zframes.pages :as pages]
            [app.pages.game.model :as model]))

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
    [:div [:span [:img.pt-icn {:src "./img/potion_red.png"}] "3/5 preasure"]]
    [:div [:span [:img.pt-icn {:src "./img/coin_gold.png"}]  "2/5 sugar"]]
    [:div [:span [:img.pt-icn {:src "./img/orc_green.png"}]  "2/5 bacteria"]]
    [:div [:span [:img.pt-icn {:src "./img/coin_gold.png"}]  "4/5 diarea"]]]])

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
