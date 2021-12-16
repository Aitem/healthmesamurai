(ns app.pages.game.end
  (:require [zframes.pages :as pages]
            [re-frame.core :as rf]
            [zframes.storage :as storage]
            [app.pages.index.model :as model]))

(def index-page ::index-page)


(rf/reg-event-fx
  index-page
  [(rf/inject-cofx ::storage/get [:player])]
  (fn [{storage :storage  db :db} [pid phase params]]
    {:db db}))

(rf/reg-sub
  index-page
  (fn [db] db))

(rf/reg-sub
  :score
  (fn [db _]
    (let [patients (get-in db [:patients])
          {:keys [dead alive]} (group-by (fn [p] (if (get-in p [:deceased :boolean])
                                                   :dead
                                                   :alive)) patients)
          score-multiplicator (count alive)
          score (reduce (fn [acc p]
                          (let [health (:health p)
                                money  (:balance p)]
                            (+ acc money health)))
                        0
                        alive)]
      (* score score-multiplicator))))


(pages/reg-subs-page
 index-page
 (fn [{d :d :as  page} _]
   (let [score   @(rf/subscribe [:score])]
     [:div.inner.rpgui-container.framed.relative
      [:h1 {:style {:font-size "250%"}} "Игра окончена!"]
      [:hr.golden]
      [:p (str "Вы набрали " score " очков")]
      [:hr]
      [:br]
      [:div.rpgui-center
       [:div {:style {:width "300px" :margin "0 auto"}}]]
      [:br]
      [:div.rpgui-center
       [:button.rpgui-button.rpgui-cursor-default
        {:on-click #(rf/dispatch [::model/start-game @(rf/subscribe [::model/practitioner-name])])}
        [:p "Сыграть снова"]]]
      [:br]])))
