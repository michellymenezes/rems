(ns rems.api.workflows
  (:require [clj-time.format :as time-format]
            [clojure.test :refer [deftest is]]
            [compojure.api.sweet :refer :all]
            [rems.api.applications :refer [Reviewer get-reviewers]]
            [rems.api.util]
            [rems.db.core :as db]
            [rems.db.workflow :as workflow]
            [rems.util :refer [getx-user-id]]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [clj-time.core :as time-core])
  (:import (org.joda.time DateTime)))

(def UserId s/Str)

(s/defschema Actor
  {:actoruserid UserId
   :round s/Num
   :role (s/enum "approver" "reviewer")})

(s/defschema WorkflowLicense
  {:type s/Str
   :start DateTime
   :textcontent s/Str
   :localizations [s/Any]
   :end (s/maybe DateTime)})

(def db-formatter (time-format/formatter "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZZ"))

(defn parse-db-time [s]
  (when s (time-format/parse db-formatter s)))

(deftest test-parse-db-time
  (is (= nil (parse-db-time nil)))
  (is (= (time-core/date-time 2019 1 30 7 56 38 627) (parse-db-time "2019-01-30T09:56:38.627616+02:00"))))

(defn format-license [license]
  (-> license
      (select-keys [:type :textcontent :localizations])
      (assoc :start (parse-db-time (:start license)))
      (assoc :end (parse-db-time (:endt license)))))

(s/defschema Workflow
  {:id s/Num
   :organization s/Str
   :owneruserid UserId
   :modifieruserid UserId
   :title s/Str
   :final-round s/Num
   :workflow s/Any
   :start DateTime
   :end (s/maybe DateTime)
   :active s/Bool
   :actors [Actor]
   :licenses [WorkflowLicense]})

(s/defschema Workflows
  [Workflow])

(defn- format-workflow
  [{:keys [id organization owneruserid modifieruserid title fnlround workflow start endt active? licenses]}]
  {:id id
   :organization organization
   :owneruserid owneruserid
   :modifieruserid modifieruserid
   :title title
   :final-round fnlround
   :workflow workflow
   :start start
   :end endt
   :active active?
   :licenses licenses})

(s/defschema CreateWorkflowCommand
  {:organization s/Str
   :title s/Str
   :type s/Keyword
   (s/optional-key :handlers) [UserId]
   (s/optional-key :rounds) [{:type (s/enum :approval :review)
                              :actors [UserId]}]})

(s/defschema CreateWorkflowResponse
  {:id s/Num})

; TODO: deduplicate or decouple with /api/applications/reviewers API?
(s/defschema AvailableActor Reviewer)
(s/defschema AvailableActors [AvailableActor])
(def get-available-actors get-reviewers)

(defn- get-workflows [filters]
  (doall
   (for [wf (workflow/get-workflows filters)]
     (assoc (format-workflow wf)
            ;; TODO should this be in db.workflow?
            :actors (db/get-workflow-actors {:wfid (:id wf)})))))

(defn- get-workflow [workflow-id]
  (-> workflow-id
      workflow/get-workflow
      format-workflow
      (update :licenses #(map format-license %))
      (assoc :actors (db/get-workflow-actors {:wfid workflow-id}))))

(def workflows-api
  (context "/workflows" []
    :tags ["workflows"]

    (GET "/" []
      :summary "Get workflows"
      :roles #{:owner}
      :query-params [{active :- (describe s/Bool "filter active or inactive workflows") nil}]
      :return Workflows
      (ok (get-workflows (when-not (nil? active) {:active? active}))))

    (POST "/create" []
      :summary "Create workflow"
      :roles #{:owner}
      :body [command CreateWorkflowCommand]
      :return CreateWorkflowResponse
      (ok (workflow/create-workflow! (assoc command :user-id (getx-user-id)))))

    (GET "/actors" []
      :summary "List of available actors"
      :roles #{:owner}
      :return AvailableActors
      (ok (get-available-actors)))

    (GET "/:workflow-id" []
      :summary "Get workflow by id"
      :roles #{:owner}
      :path-params [workflow-id :- (describe s/Num "workflow-id")]
      :return Workflow
      (ok (get-workflow workflow-id)))))
