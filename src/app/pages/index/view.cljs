(ns app.pages.index.view
  (:require [zframes.pages :as pages]
            [re-frame.core :as rf]
            [app.pages.index.model :as model]))



(pages/reg-subs-page
 model/index-page
 (fn [{d :d :as  page} _]
   (let [gettext (fn [e] (-> e .-target .-value))
         emit    (fn [e] (rf/dispatch [::model/practitioner-name (gettext e)])) ]
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
        [:input {:type "text"
                 :minLength 2
                 :placeholder "Ваше имя"
                 :value @(rf/subscribe [::model/practitioner-name])
                 :on-change emit}]]]
      [:br]
      [:div.rpgui-center
       [:button.rpgui-button.rpgui-cursor-default
        {:on-click #(rf/dispatch [::model/start-game @(rf/subscribe [::model/practitioner-name])])}
        [:p "Начать"]]]
      [:br]])))
