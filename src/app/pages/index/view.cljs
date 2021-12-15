(ns app.pages.index.view
  (:require [zframes.pages :as pages]
            [re-frame.core :as rf]
            [app.pages.index.model :as model]))


(pages/reg-subs-page
 model/index-page
 (fn [{d :d :as  page} _]
   [:div.inner.rpgui-container.framed.relative
    [:h1 {:style {:font-size "250%"}} "Health me, Samurai!"]
    [:hr.golden]
    [:p "2040 год, человечество окончательно победило коронавирус. Но не долго длилась радость, появился новый вирус противостояние с которым только началось. "]
    [:p "Вы Senior медбрат госпиталя святого Николая, и только от ваших действий зависит кто переживет эту ночь."]
    [:hr]
    [:br]
    [:div.rpgui-center
     [:div {:style {:width "300px" :margin "0 auto"}}
      [:label "Как тебя зовут?"]
      [:input ]]]
    [:br]
    [:div.rpgui-center
     [:button.rpgui-button.rpgui-cursor-default
      {:on-click #(rf/dispatch [::model/start-game])}
      [:p "Начать"]]]
    [:br]]))
