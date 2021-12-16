(ns app.pages.game.end
  (:require [zframes.pages :as pages]
            [re-frame.core :as rf]
            [zframes.routing :as routing]
            [zframes.storage :as storage]
            [app.pages.index.model :as model]))

(def index-page ::index-page)

(rf/reg-event-fx
 ::restart
 (fn [{db :db} [evid _]]
   {::routing/redirect {:ev :app.pages.index.model/index-page}}))

(rf/reg-event-fx
  index-page
  [(rf/inject-cofx ::storage/get [:player])]
  (fn [{storage :storage  db :db} [pid phase params]]
    {:db db}))

(rf/reg-sub
  index-page
  (fn [db] db))

(defn score-calculator
  [keyword coll]
  (reduce (fn [acc p]
            (let [param (keyword p)]
              (+ acc param)))
          0
          coll))

(rf/reg-sub
  :stats
  (fn [db _]
    (let [patients                 (-> db
                                       (get-in [:patients])
                                       vals)
          {:keys [dead alive]}     (group-by (fn [p] (prn p)
                                               (if (get-in p [:deceased :boolean])
                                                 :dead
                                                 :alive)) patients)
          score-multiplicator      (count alive)
          patients-alive           (count alive)
          patients-died            (count dead)
          money-left               (score-calculator :balance alive)
          patient-health-left      (score-calculator :health alive)
          dead-patients-money-left (score-calculator :balance dead)
          score                    (- (+ money-left patient-health-left)
                                      dead-patients-money-left)
          total-score              (* score score-multiplicator)]
      {:patients-alive patients-alive
       :patients-died  patients-died
       :money-left money-left
       :patient-health-left  patient-health-left
       :total-score total-score
       :score score
       :dead-patients-money-left dead-patients-money-left})))


(pages/reg-subs-page
 index-page
 (fn [{d :d :as  page} _]
   (let [stats   @(rf/subscribe [:stats])]
     [:div.inner.rpgui-container.framed.relative
      [:h1 {:style {:font-size "250%"}} "Игра окончена!"]
      [:hr.golden]
      [:table {:style {:width "100%"
                       :table-layout :fixed
                       :border "none"
                       :text-color "white"}}
       [:tbody
        [:tr
         [:td.score-td.score-right-td [:p "Выжило пациентов: "]]
         [:td.score-td.score-left-td [:p (:patients-alive stats)]]]
        [:tr
         [:td.score-td.score-right-td [:p "Умерло пациентов: "]]
         [:td.score-td.score-left-td [:p (:patients-died  stats)]]]
        [:tr
         [:td.score-td.score-right-td [:p "Денег осталось: "]]
         [:td.score-td.score-left-td [:p (:money-left     stats)]]]
        [:tr
         [:td.score-td.score-right-td [:p "Оставшееся здоровье: "]]
         [:td.score-td.score-left-td [:p (:patient-health-left stats)]]]
        [:tr
         [:td.score-td.score-right-td [:p "Денбги мертвецов: "]]
         [:td.score-td.score-left-td [:p (:dead-patients-money-left stats)]]]
        [:tr
         [:td.score-td.score-right-td [:p "Счет: "]]
         [:td.score-td.score-left-td [:p (:score stats)]]]]]
      [:hr]
      [:div {:style {:display :flex :justify-content :center}}
       [:div
        [:p (str "Финальный счет = "
                 (:patient-health-left stats)
                 " + " (:money-left     stats)
                 " - " (:dead-patients-money-left stats)
                 " = " (:total-score stats))]]]
      [:br]
      [:div.rpgui-center
       [:div {:style {:width "300px" :margin "0 auto"}}]]
      [:br]
      [:div.rpgui-center
       [:button.rpgui-button.rpgui-cursor-default
        {:on-click #(rf/dispatch [::restart])}
        "Сыграть снова"]]
      [:br]])))
