# org.photonsphere/fulcro.ui.bulma

A fulcro.ui.bootstrap3 inspired Clojure library for Bulma, to be used with Fulcro -- Mari Donkers (photonsphere.org).

(Twitter: @maridonkers | Google+: +MariDonkers | GitHub: maridonkers)"

## Github

The source code to this project can be found on Github: [Fulcro-Bulma](https://github.com/maridonkers/fulcro-bulma)

## Usage

In your project.clj use the following dependency:

`[org.photonsphere/fulcro.ui.bulma "0.1.0"]`

## Examples

Taken and adapted from the Fulcro [Template Project](https://github.com/fulcrologic/fulcro-template).

### Navbar

Note: Source code incomplete, only showing Fulcro-Bulma related parts.

```clojure
(ns org.photonsphere.example.ui.root
  (:require [fulcro.client.routing :refer [defrouter]]
            [fulcro.client.mutations :as m]
            [fulcro.client.dom :as dom]
            [fulcro-css.css :as css]
            [fulcro.client.logging :as log]
            [org.photonsphere.example.ui.html5-routing :as r]
            [org.photonsphere.example.ui.login :as l]
            [org.photonsphere.example.ui.user :as user]
            [org.photonsphere.example.ui.main :as main]
            [org.photonsphere.example.ui.preferences :as prefs]
            [org.photonsphere.example.ui.new-user :as nu]
            [org.photonsphere.example.api.mutations :as api]
            [fulcro.client.primitives :as prim :refer [defsc]]
            [fulcro.i18n :refer [tr]]
            [org.photonsphere.fulcro.ui.bulma :as b]))

(def render-fcns
  {:navbar-i18n-render-fcn (fn [this]
                             (let [i18n-fcn (fn [lc]
                                              (prim/transact! this
                                                              `[(m/change-locale {:lang ~lc})]))]
                               (dom/div #js {:className "field is-grouped"}
                                        (dom/a #js {:href "#"
                                                    :onClick (fn [] (i18n-fcn :en))} (tr "en"))
                                        (dom/span nil "|")
                                        (dom/a #js {:href "#"
                                                    :onClick (fn [] (i18n-fcn :es))} (tr "es")))))
   :navbar-philosophers-render-fcn
   (fn [_]
     [(dom/img #js {:key :karlrpopper-image
                    :src "https://images.gr-assets.com/authors/1480329278p4/14772624.jpg"})
      (dom/p #js {:key :karlrpopper-caption} "Karl R. Popper")])})

;; Use keywords formatted as navbar-pagename for the page links.
(def top-navbar
  (b/navbar :top-navbar
            :navbar-main
            (b/navbar-brand :top-navbar-brand
                            [(b/navbar-sublink :navbar-i18n :navbar-i18n-render-fcn)])
            (b/navbar-menu :top-navbar-menu
                           (b/navbar-menu-start :top-navbar-menu-start
                                                [(b/navbar-link :navbar-main
                                                                (tr "Main")
                                                                {:displayed? false})
                                                 (b/navbar-link :navbar-preferences
                                                                (tr "Preferences")
                                                                {:displayed? false})
                                                 (b/navbar-sublink :navbar-sublink
                                                                   :navbar-philosophers-render-fcn)])
                           (b/navbar-menu-end :top-navbar-menu-end
                                              [(b/navbar-link :navbar-dummy (tr "Dummy (stateless)")
                                                              {:displayed? false :stateful? false})
                                               (b/navbar-dropdown :navbar-dropdown-settings
                                                                  (tr "Settings")
                                                                  {:displayed? false}
                                                                  [(b/navbar-link :navbar-dropdown-account
                                                                                  (tr "Account"))
                                                                   (b/navbar-link :navbar-dropdown-profile
                                                                                  (tr "Profile"))
                                                                   (b/navbar-divider :navbar-dropdown-divider)
                                                                   (b/navbar-link :navbar-logout
                                                                                  (tr "Log out"))])]))
            nil))
            
(defsc TopNavbar [this
                  {:keys [navbar] :as props}
                  {:keys [logged-in? current-route] :as cprops}]
  {:initial-state (fn [params]
                    {:navbar top-navbar})
   :ident (fn [] [:top-navbar/by-id :singleton])
   :query [{:navbar (prim/get-query b/Navbar)}]
   :css-include [b/Navbar]}
  (let [current-navbar-link (keyword (str "navbar-" (name current-route)))
        logout-fcn (fn [disabled?]
                     (prim/transact! this
                                     `[(api/logout {})
                                       (r/set-route! {:handler :login}) :current-user]))
        i18n-fcn (fn [lc]
                   (prim/transact! this
                                   `[(m/change-locale {:lang ~lc})]))
        ui-navbar (b/ui-navbar navbar
                               {:onSelect (fn [link]
                                            (case link
                                              :navbar-i18n-en (i18n-fcn :en)
                                              :navbar-i18n-es (i18n-fcn :es)
                                              :navbar-main (r/nav-to! this :main)
                                              :navbar-preferences (r/nav-to! this :preferences)
                                              :navbar-dropdown-account (r/nav-to! this :main)
                                              :navbar-dropdown-profile (r/nav-to! this :preferences)
                                              :navbar-logout (logout-fcn true))
                                            (log/info (str "TopNavbar:: Selected NAVBAR link is: " link
                                                           ", logged-in? " logged-in?)))
                                :stateful? true
                                :attrs {:navbar {:style #js {:background-color "lightgray"}}
                                        :brand-img {:src "https://bulma.io/images/bulma-logo.png"
                                                    :style #js {:width "112px"
                                                                :height "28px"}
                                                    :alt "Bulma"}
                                        ;; :dropdown-container {:className "is-hoverable"}
                                        :dropdown {:className "is-boxed"}}
                                :render-fcns render-fcns})]
    (log/info (str "TopNavbar:: logged-in? " logged-in? ", current-route " current-navbar-link))
    ;; Do not execute the transact! on the server (hence the reader conditional)...
    #?(:cljs (prim/transact! this
                             `[(b/display-navbar-link ~{:target :navbar-main
                                                        :displayed? logged-in?}) 
                               (b/display-navbar-link ~{:target :navbar-preferences
                                                        :displayed? logged-in?})
                               (b/display-navbar-link ~{:target :navbar-dummy
                                                        :displayed? logged-in?})
                               (b/display-navbar-dropdown ~{:id :navbar-dropdown-settings
                                                            :displayed? logged-in?})
                               (b/set-active-navbar-link ~{:id :top-navbar
                                                           :target current-navbar-link})]))
    ui-navbar))
    
(def ui-top-navbar (prim/factory TopNavbar))

(defsc Root [this {:keys [ui/ready? ui/react-key logged-in? pages top-navbar]
                   :or {react-key "ROOT"}}]
  {:initial-state (fn [p] (merge
                           {; Is there a user logged in?
                            :logged-in? false
                                        ; Is the UI ready for initial render?
                                        ; This avoids flicker while we figure out
                                        ; if the user is already logged in
                            :ui/ready? false
                                        ; What are the details of the logged in user
                            :current-user nil
                            ;; :root/modals  (prim/get-initial-state Modals {})
                            :pages (prim/get-initial-state Pages nil)
                            :top-navbar (prim/get-initial-state TopNavbar nil)
                            }
                           r/app-routing-tree))
   :query [:ui/react-key :ui/ready? :logged-in?
           {:current-user (prim/get-query user/User)}
           #_{:root/modals (prim/get-query Modals)}
                                        ; TODO: Check if this [routing-tree-key] is
                                        ; needed...seemed to affect initial state from SSR.
           fulcro.client.routing/routing-tree-key
           :ui/loading-data
           {:pages (prim/get-query Pages)}
           {:top-navbar (prim/get-query TopNavbar)}]
   :css-include [TopNavbar]}
  (let [current-route (get-in pages [:fulcro.client.routing/current-route :id])]
    (log/info (str "ROOT:: logged-in? " logged-in? ", current-route " current-route))
  (dom/div #js {:key react-key}
           (log/info (str "ROOT:: pages=" pages))
           #_(ui-navbar this)
           (ui-top-navbar (prim/computed top-navbar {:logged-in? logged-in?
                                                     :current-route current-route}))
           (when ready?
             (ui-pages pages)))))
```

### Dropdown

Note: Source code incomplete, only showing Fulcro-Bulma related parts.

```clojure
(ns org.photonsphere.example.ui.preferences
  (:require [fulcro.client.primitives :as prim :refer [defsc]]
            [fulcro.client.logging :as log]
            [fulcro.client :as u]
            [fulcro.i18n :refer [tr]]
            [fulcro.client.dom :as dom]
            [fulcro.client.mutations :as m]
            [org.photonsphere.fulcro.ui.bulma :as b]))

(def render-fcns {:philosophers-render-fcn 
                  (fn [_]
                    [(dom/img #js {:key :karlrpopper-image
                                   :src "https://images.gr-assets.com/authors/1480329278p4/14772624.jpg"})
                     (dom/p #js {:key :karlrpopper-caption} (str "“" (tr "No rational argument will have a rational effect on a man who does not want to adopt a rational attitude.") "”― Karl R. Popper"))])})

(def car-make-dropdown
  (b/dropdown :car-make
              (tr "Car Make")
              [(b/dropdown-item :ford (tr "Ford"))
               (b/dropdown-item :opel (tr "Opel"))
               (b/dropdown-item :tesla (tr "Tesla"))
               (b/dropdown-divider :intermezzo)
               (b/dropdown-subitem :philosophers :philosophers-render-fcn)
               (b/dropdown-divider :french)
               (b/dropdown-item :renault (tr "Renault"))
               (b/dropdown-item :peugeot (tr "Peugeot"))
               (b/dropdown-divider :german)
               (b/dropdown-item :volkswagen (tr "Volkswagen"))
               (b/dropdown-item :mercedes (tr "Mercedes"))
               (b/dropdown-item :bmw (tr "BMW"))
               (b/dropdown-item :porsche (tr "Porsche"))
               (b/dropdown-divider :italian)
               (b/dropdown-item :fiat (tr "Fiat"))
               (b/dropdown-item :ferrari (tr "Ferrari"))
               (b/dropdown-item :lamborghini (tr "Lamborghini"))
               (b/dropdown-divider :japanese)
               (b/dropdown-item :honda (tr "Honda"))
               (b/dropdown-item :toyota (tr "Toyota"))
               (b/dropdown-divider :korean)
               (b/dropdown-item :hyundai (tr "Hyundai"))]))
               
(defn multi-font-size-block
  "Renders a block with the same text in multiple font sizes."
  [txt]
  (dom/article #js {:className "tile is-child notification is-danger"}
               (dom/p nil
                      (dom/b #js {:className "is-size-1"}
                             txt))
               (dom/p nil
                      (dom/b #js {:className "is-size-2"}
                             txt))
               (dom/p nil
                      (dom/b #js {:className "is-size-3"}
                             txt))
               (dom/p nil
                      (dom/b #js {:className "is-size-4"}
                             txt))
               (dom/p nil
                      (dom/b #js {:className "is-size-5"}
                             txt))
               (dom/p nil
                      (dom/b #js {:className "is-size-6"}
                             txt))
               (dom/p nil
                      (dom/b #js {:className "is-size-7"}
                             txt))))
               
(defsc PreferencesPage [this {:keys [id car-make-dropdown]}]
  {:initial-state (fn [_]
                    {:id :preferences
                     :car-make-dropdown car-make-dropdown})
   :query [:id
           {:car-make-dropdown (prim/get-query b/Dropdown)}]
   :ident (fn [] [:preferences :page])}
  (let [facebook-button-fcn (fn []
                              (log/info "Facebook button clicked.")
                              #?(:cljs (js/alert (tr "Facebook button clicked!"))))
        dropdown (b/ui-dropdown car-make-dropdown
                                {:onSelect (fn [item]
                                             (log/info (str "Current item is: " item)))
                                 :stateful? true
                                 :attrs {;:dropdown {:className "is-hoverable"}
                                         :button {:className "is-primary"}
                                        ; :button-icon-span {:className "is-large"}
                                         :button-icon {:className "fa fa-plus"}
                                        ; :items {:className "notification is-danger"}
                                         }
                                 :render-fcns render-fcns})
        button (dom/button #js {:className "button is-success"
                                :onClick facebook-button-fcn}
                           (dom/span #js {:className "icon"}
                                     (dom/i #js {:className "fa fa-facebook"}))
                           (dom/span nil (tr "Facebook")))]
    (dom/div #js {:className "section"}
             (dom/div #js {:className "container is-fluid"}
                      (dom/div #js {:className "tile is-ancestor"}
                               (dom/div #js {:className "tile is-8 is-parent"}
                                        (multi-font-size-block (tr "Preferences page")))
                               (dom/div #js {:className "tile is-2 is-parent"}
                                        (dom/article #js {:className "tile is-child"}
                                                     dropdown))
                               (dom/div #js {:className "tile is-2 is-parent"}
                                        (dom/article #js {:className "tile is-child"}
                                                     button)))))))
```

### SSR (co-located CSS)

Note: Source code incomplete, only showing Fulcro-Bulma related parts.

```clojure
(ns org.photonsphere.example.server
  (:require
   [fulcro.server :as core]
   [fulcro-css.css :as css]
   [com.stuartsierra.component :as component]

   [org.httpkit.server :refer [run-server]]
   [fulcro.server :as server]
   org.photonsphere.example.api.read
   org.photonsphere.example.api.mutations
   [fulcro.client.primitives :as prim :refer [get-ident tree->db db->tree factory get-query merge-component]]
   [org.photonsphere.example.api.user-db :as users]

   [org.photonsphere.example.ui.root :as root]
   [org.photonsphere.example.ui.html5-routing :as routing]
   [fulcro.client :as fc]

   [bidi.bidi :as bidi]
   [taoensso.timbre :as timbre]

   [ring.middleware.session :as session]
   [ring.middleware.session.store :as store]
   [ring.middleware.resource :as resource]
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.gzip :refer [wrap-gzip]]
   [ring.middleware.not-modified :refer [wrap-not-modified]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.resource :refer [wrap-resource]]
   [ring.middleware.cookies :as cookies]

   [ring.util.request :as req]
   [ring.util.response :as response]
   [fulcro.util :as util]
   [fulcro.server-render :as ssr]
   [org.photonsphere.example.ui.user :as user]
   [clojure.string :as str]
   [fulcro.i18n :as i18n]
   [fulcro.client.mutations :as m]
   [fulcro.client.dom :as dom]
   [org.photonsphere.fulcro.ui.bulma :refer [bulma-ssr-css]]))
   
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; SERVER-SIDE RENDERING
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn top-html
  "Render the HTML for the SPA. There is only ever one kind of HTML to
  send, but the initial state and initial app view may vary. This
  function takes a normalized client database and a root UI class and
  generates that page."
  [normalized-client-state root-component-class]
  ; props are a "view" of the db. We use that to generate the view,
  ; but the initial state needs to be the entire db
  (let [props                (db->tree (get-query root-component-class) normalized-client-state normalized-client-state)
        root-factory         (factory root-component-class)
        app-html             (dom/render-to-str (root-factory props))
        initial-state-script (ssr/initial-state->script-tag normalized-client-state)]
    (str "<!DOCTYPE) html>\n"
      "<html lang='en'>\n"
      "<head>\n"
      "<meta charset='UTF-8'>\n"
      "<meta name='viewport' content='width=device-width, initial-scale=1'>\n"
      "<link href='https://maxcdn.bootstrapcdn.com/font-awesome/4.7.0/css/font-awesome.min.css' rel='stylesheet'>\n"
      "<link href='https://cdnjs.cloudflare.com/ajax/libs/bulma/0.6.1/css/bulma.min.css' rel='stylesheet'>\n"
      (bulma-ssr-css)
      initial-state-script
      "<title>Example Home Page (Dev Mode)</title>\n"
      "</head>\n"
      "<body>\n"
      "<div id='app'>"
      app-html
      "</div>\n"
      "<script src='js/example.js' type='text/javascript'></script>\n"
      "</body>\n"
      "</html>\n")))
```
