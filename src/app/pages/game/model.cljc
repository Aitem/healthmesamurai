(ns app.pages.game.model
  (:require [re-frame.core :as rf]
            [zframes.storage :as storage]
            [zframes.pages :as pages]))

(def index-page ::index-page)

(rf/reg-event-fx
 index-page
 [(rf/inject-cofx ::storage/get [:player])]
 (fn [{storage :storage  db :db} [pid phase params]]
   {:db (merge db storage)
    :json/fetch {:uri "/Patient"
                 :params {:general-practitioner (get-in storage [:player :id])}
                 :req-id ::pts}}))

(rf/reg-sub
 index-page
 :<- [:xhr/response ::pts]
 (fn [pts _]
   {:pts (->> pts :data :entry (map :resource))
    :aidbox [{:id      "111"
              :name    "Insulin"
              :img     "./img/insulin.png"
              :price   2
              :effects {:diarea      -1
                        :sugar        3
                        :bacteria     1}}
             {:id      "22"
              :name    "Amoxicillin"
              :img     "./img/amoxicillin.png"
              :price   2
              :effects {:temperature  1
                        :diarea      -1
                        :bacteria     2}}
             ]}))
