(ns app.pages.game.model
  (:require [re-frame.core :as rf]
            [zframes.storage :as storage]
            [zframes.pages :as pages]))

(def index-page ::index-page)


(def aidbox
  {"11" {:id      "11"
         :name    "Insulin"
         :img     "./img/insulin.png"
         :price   2
         :effects {:diarea      -1
                   :sugar        3
                   :bacteria     1}}

   "22" {:id      "22"
         :name    "Amoxicillin"
         :img     "./img/amoxicillin.png"
         :price   2
         :effects {:temperature  1
                   :diarea      -1
                   :bacteria     2}}})

(rf/reg-event-fx
 index-page
 [(rf/inject-cofx ::storage/get [:player])]
 (fn [{storage :storage  db :db} [pid phase params]]
   {:db (-> db
            (merge storage)
            (assoc :aidbox aidbox))
    :json/fetch {:uri "/Patient"
                 :params {:general-practitioner (get-in storage [:player :id])}
                 :success {:event ::save-patients}
                 :req-id ::pts}}))


(defn patients-map [resp]
  (->> resp :data :entry (map :resource)
       (reduce (fn [acc pt] (assoc acc (:id pt) pt)) {})))

(def drag-zone-cfg {:drop-dispatch [:my-drop-dispatch] :drop-marker   :my-drop-marker})

(rf/reg-event-fx
 ::save-patients
 (fn [{db :db} [_ resp]]
   (let [pts (patients-map resp)
         init-droppable (map  (fn [[k v]] [:dnd/initialize-drop-zone (keyword k) drag-zone-cfg]) pts)]
     {:dispatch-n  init-droppable
      :db (assoc db :patients pts)})))

(rf/reg-sub
 ::aidbox
 (fn [db _]
   (:aidbox db)))

(rf/reg-sub
 index-page
 :<- [:xhr/response ::pts]
 :<- [::aidbox]
 (fn [[resp aids] _]
   {:pts (patients-map resp)
    :aidbox aids}))
