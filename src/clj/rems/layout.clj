(ns rems.layout
  (:require [compojure.response]
            [rems.context :as context]
            [rems.text :refer :all]
            [rems.language-switcher :refer [language-switcher]]
            [hiccup.element :refer [link-to]]
            [hiccup.page :refer [html5 include-css include-js]]
            [markdown.core :refer [md-to-html-string]]
            [ring.util.http-response :as response]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]])
  (:import compojure.response.Renderable))


(defn url-dest
  [dest]
  (str context/*root-path* dest))

(defn nav-link
  ([path title]
   (nav-link path title "nav-link"))
  ([path title nav-classes]
   (link-to {:class nav-classes} (url-dest path) title))
  ([path title page-name li-name]
   (nav-link path title (if (= page-name li-name) "nav-link active" "nav-link"))))

(defn nav-item
  [path title page-name li-name]
  [:li.nav-item
   (nav-link path title page-name li-name)])

(defn primary-nav
  [page-name user]
  [:ul.nav.navbar-nav
   (if user
     (nav-item "/catalogue" (text :t.navigation/catalogue) page-name "catalogue")
     (nav-item "/" (text :t.navigation/home) page-name "home"))
   (nav-item "/about" (text :t.navigation/about) page-name "about")])

(defn secondary-nav
  [user]
  [:div.secondary-navigation.navbar-nav.navitem
   (when user
     [:div.user
      [:div.fa.fa-user]
      [:div.user-name (str user " / ")]
      [:div.logout-link
       (nav-link "/Shibboleth.sso/Logout?return=%2F" (text :t.navigation/logout))]])
   (language-switcher)])

(defn navbar
  [page-name user]
  [:nav.navbar {:role "navigation"}
   [:button.navbar-toggler.hidden-sm-up
    {:type "button" :data-toggle "collapse" :data-target "#collapsing-navbar"}
    "&#9776;"]
   [:div#collapsing-navbar.collapse.navbar-toggleable-xs
    (primary-nav page-name user)
    (secondary-nav user)]])

(defn footer []
  [:footer (text :t/footer)])

(defn logo []
  [:div.logo])

(defn page-template
  [page-name nav content footer]
  (html5 [:head
          [:META {:http-equiv "Content-Type" :content "text/html; charset=UTF-8"}]
          [:META {:name "viewport" :content "width=device-width, initial-scale=1"}]
          [:title "Welcome to rems"]
          (include-css "/assets/bootstrap/css/bootstrap.min.css")
          (include-css "/assets/font-awesome/css/font-awesome.min.css")
          (include-css "/css/screen.css")

          [:body
           [:div.wrapper
            [:div.container nav]
            (logo)
            [:div.container content]
            [:div.push]]
           footer
           (include-js "/assets/jquery/jquery.min.js")
           (include-js "/assets/tether/dist/js/tether.min.js")
           (include-js "/assets/bootstrap/js/bootstrap.min.js")]]))

(defn render
  "renders HTML generated by Hiccup

   params: :status -- status code to return, defaults to 200
           :headers -- map of headers to return, optional
           :content-type -- optional, defaults to \"text/html; charset=utf-8\"
           :bare -- don't include navbar and footer"
  [page-name content & [params]]
  (let [nav (when-not (:bare params)
              (navbar page-name context/*user*))
        footer (when-not (:bare params)
                 (footer))
        content-type (:content-type params "text/html; charset=utf-8")
        status (:status params 200)
        headers (:headers params {})]
      (response/content-type
       {:status status
        :headers headers
        :body (page-template page-name nav content footer)}
       content-type)))

(defn error-content
  [error-details]
  [:div.container-fluid
   [:div.row-fluid
    [:div.col-lg-12
     [:div.centering.text-center
      [:div.text-center
       [:h1
        [:span.text-danger (str "Error: " (error-details :status))]
        [:hr]
        (when-let [title (error-details :title)]
          [:h2.without-margin title])
        (when-let [message (error-details :message)]
          [:h4.text-danger message])]]]]]])

(defn error-page
  "error-details should be a map containing the following keys:
   :status - error status
   :title - error title (optional)
   :message - detailed error message (optional)
   :bare - don't render navbar and footer (optional)

   returns a response map with the error page as the body
   and the status specified by the status key"
  [error-details]
  (render "error page" (error-content error-details) error-details))
