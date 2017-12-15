(ns rems.applications
  (:require [clojure.string :as string]
            [compojure.core :refer [GET defroutes]]
            [rems.db.applications :refer [get-my-applications]]
            [rems.guide :refer :all]
            [rems.layout :as layout]
            [rems.text :refer [localize-state localize-time text]]))

(defn- applications-item [app]
  [:tr.application
   [:td {:data-th (text :t.applications/application)} (:id app)]
   [:td {:data-th (text :t.applications/resource)} (string/join ", " (map :title (:catalogue-items app)))]
   [:td {:data-th (text :t.applications/state)} (text (localize-state (:state app)))]
   [:td {:data-th (text :t.applications/created)} (localize-time (:start app))]
   [:td [:a.btn.btn-primary
         {:href (str "/form/" (:id app))}
         (text :t/applications.view)]]])

(defn- applications
  ([]
   (applications (get-my-applications)))
  ([apps]
   (if (empty? apps)
    [:div.applications.alert.alert-success (text :t/applications.empty)]
    [:table.rems-table.applications
     [:tr
      [:th (text :t.applications/application)]
      [:th (text :t.applications/resource)]
      [:th (text :t.applications/state)]
      [:th (text :t.applications/created)]]
     (for [app apps]
       (applications-item app))])))

(defn- applications-page []
  (layout/render
   "applications"
   (applications)))

(defroutes applications-routes
  (GET "/applications" [] (applications-page)))

(defn guide
  []
  (list
   (example "applications empty"
            (applications []))
   (example "applications"
            (applications
             [{:id 1 :catalogue-items [{:title "Draft application"}] :state "draft" :applicantuserid "alice"}
              {:id 2 :catalogue-items [{:title "Applied application"}] :state "applied" :applicantuserid "bob"}
              {:id 3 :catalogue-items [{:title "Approved application"}] :state "approved" :applicantuserid "charlie"}
              {:id 4 :catalogue-items [{:title "Rejected application"}] :state "rejected" :applicantuserid "david"}
              {:id 5 :catalogue-items [{:title "Closed application"}] :state "closed" :applicantuserid "ernie"}]))))
