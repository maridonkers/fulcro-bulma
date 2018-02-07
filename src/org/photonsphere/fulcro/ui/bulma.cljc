(ns org.photonsphere.fulcro.ui.bulma
  "A fulcro.ui.bootstrap3 inspired Clojure library for Bulma, to be used with Fulcro -- Mari Donkers (photonsphere.org).

  (Twitter: @maridonkers | Google+: +MariDonkers | GitHub: maridonkers)"
  (:require [fulcro.client.dom :as dom]
            [fulcro-css.css :as css]
            [fulcro.client.primitives :as prim :refer [defsc]]
            [fulcro.i18n :refer [tr tr-unsafe]]
            [fulcro.client.mutations :as m :refer [defmutation]]
            ;; #?(:clj [clojure.future :refer :all])
            [clojure.string :as str]))

#?(:clj (defn- clj->js [m] m))

(defn- condense-string
  "Condenses string by replacing superfluous whitespaces with a single space and also trims it."
  [s]
  (-> s
      (str/replace #"\s{2,}" " ")
      (str/trim)))

(defn- attrs-merge
  "To be used with merge-with, to overwrite some attributes --the ones matched by one of the regular expressions specified in the rules parameter-- that have the same name in val-in-result and val-in-latter parameters. The rules parameter contains a vector with regexp rules, e.g. as follows (to replace a default Font Awesome icon): [#\"fa\\s+\\S+\"]. Beware: the regexp rules dictates which parts are deleted in the result (which is always done when there's a regexp, even when there's nothing to replace it in latter)."
  ([val-in-result val-in-latter]
   (attrs-merge val-in-result val-in-latter nil))
  ([val-in-result val-in-latter rules]
   (if (some? rules)
     (let [val-out-result (reduce (fn [result rule]
                                    (if (re-find rule result)
                                      (str/replace result rule "")
                                      result))
                                  val-in-result
                                  rules)]
       (str (condense-string val-out-result) " " val-in-latter))
     (str val-in-result " " val-in-latter))))

(defn- report-error
  "Reports an error as HTTP."
  [err msg]
  (dom/code nil (str "HTTP error " err " (" msg ")")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DROPDOWN
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def dropdown-table :bulma.dropdown/by-id)
(def dropdown-item-table :bulma.dropdown-item/by-id)

(defn dropdown-item
  "Define the state for an item of a dropdown. The label will be run
  through `tr-unsafe`, so it can be internationalized."
  [id label] {::id id
              ::label label
              ::disabled? false})

(defn dropdown-subitem
  "Define the state for a subitem (which has arbitrary content in a div)
  of a dropdown, which will be rendered by a render function,
  designated with the fcn-kw keyword. Must have a unique ID."
  [id fcn-kw] {::id id
               ::label ::subitem
               ::disabled? false
               ::render-fcn-kw fcn-kw})

(defn dropdown-divider
  "Creates a divider between items. Must have a unique ID."
  [id] {::id id
        ::label ::divider
        ::disabled? false})

(defn dropdown
  "Creates a dropdown's state. Create items with dropdown-item or
  dropdown-divider. Must have a unique ID."
  [id label items] {::id id
                    ::active-item nil
                    ::label label
                    ::items items
                    ::open? false
                    ::disabled? false})

(defn dropdown-ident [id-or-props]
  (if (map? id-or-props)
    [dropdown-table (::id id-or-props)]
    [dropdown-table id-or-props]))

(defn dropdown-item-ident [id-or-props]
  (if (map? id-or-props)
    (dropdown-item-ident (::id id-or-props))
    [dropdown-item-table id-or-props]))

(m/defmutation set-dropdown-disabled
  "Mutation. Set the disabled? flag to true/false."
  [{:keys [id disabled?]}]
  (action [{:keys [state]}]
          (let [kpath (conj (dropdown-ident id) ::disabled?)]
            (swap! state assoc-in kpath disabled?))))

(m/defmutation set-dropdown-open
  "Mutation. Set the open? flag to true/false to open/close the dropdown."
  [{:keys [id open?]}]
  (action [{:keys [state]}]
          (let [kpath (conj (dropdown-ident id) ::open?)]
            (swap! state assoc-in kpath open?))))

(m/defmutation set-dropdown-item-disabled
  "Mutation. Set the disabled? flag for one of the items in a dropdown.

  item-id - the ID of the dropdown item;
  disabled? - true or false."
  [{:keys [item-id disabled?]}]
  (action [{:keys [state]}]
          (swap! state update-in (dropdown-item-ident item-id) assoc ::disabled? disabled?)))

(m/defmutation set-dropdown-active-item
  "Mutation. Set active-item to the indicated item of the a dropdown.

  id - the ID of the dropdown;
  item-id - the ID of the dropdown item."
  [{:keys [id item-id]}]
  (action [{:keys [state]}]
          (swap! state update-in (dropdown-ident id) assoc ::active-item item-id)))

(defn- close-all-dropdowns-impl [dropdown-map]
  (reduce (fn [m id] (assoc-in m [id ::open?] false)) dropdown-map (keys dropdown-map)))

(m/defmutation close-all-dropdowns
  "Mutations: Close all dropdowns (globally)"
  [ignored]
  (action [{:keys [state]}]
          (swap! state update dropdown-table close-all-dropdowns-impl)))

(defsc DropdownItem [this
                     {:keys [::id ::label ::disabled? ::render-fcn-kw] :as props}
                     {:keys [onSelect active? attrs render-fcn] :as cprops}]
  {:query [::id ::label ::disabled? ::render-fcn-kw]
   :ident (fn [] (dropdown-item-ident props))}
  (let [diid (name id)
        items-attrs (merge-with attrs-merge
                                {:key id
                                 :id (str "dropdown-item-" diid)
                                 :className (cond-> "dropdown-item"
                                              active? (str " is-active")
                                              disabled? (str " is-disabled"))
                                 :onClick (fn [evt]
                                            (.stopPropagation evt)
                                            (when-not disabled? (onSelect id))
                                            false)}
                                (:items attrs))]
    (case label
      ::divider (dom/hr #js {:key id
                             :className "dropdown-divider"})
      ::subitem (dom/div #js {:key id
                              :className "dropdown-item"}
                         (if render-fcn
                           (render-fcn this)
                           (report-error 501 render-fcn-kw)))
      (dom/a (clj->js items-attrs)
             (tr-unsafe label)))))

(let [ui-dropdown-item-factory (prim/factory DropdownItem {:keyfn ::id})]
  (defn ui-dropdown-item
    "Render a dropdown item. The props are the state props of the
     dropdown item. The additional arguments are in cprops:

     onSelect - the function to call when a menu item is selected;
     active? - render this item as active;
     attrs - will be added to dropdown item's attributes;
     render-fcn - render function (gets DropdownItem props as a parameter).

     Use for each single item of the dropdown, which can either be an
     a(nchor) or a div.

     While the dropdown-item can be used as an anchor link <a>, you can
     also use a <div> and insert almost any type of content."
    [props {:keys [onSelect active? attrs render-fcn] :as cprops}]
    (ui-dropdown-item-factory (prim/computed props cprops))))

(defsc Dropdown [this
                 {:keys [::id ::label ::active-item ::open? ::disabled? ::items] :as props}
                 {:keys [onSelect stateful? attrs render-fcns] :as cprops}]
  {:query [::id ::label ::active-item ::open? ::disabled?
           {::items (prim/get-query DropdownItem)}]
   :ident (fn [] (dropdown-ident props))}
  (let [did (str "dropdown-" (name id))
        dbid (str did "-button")
        dmid (str did "-menu")
        dropdown-attrs (:dropdown attrs)
        is-hoverable? (and dropdown-attrs
                           (not (str/blank? (:className dropdown-attrs)))
                           (str/includes? (str/lower-case(:className dropdown-attrs))
                                          "is-hoverable"))
        active-item-label (->> items
                               (some #(and (= active-item (::id %)) %))
                               ::label)
        label (if (and active-item-label stateful?) active-item-label label)
        onSelect (fn [item-id]
                   (prim/transact! this
                                   `[(close-all-dropdowns {})
                                     (set-dropdown-active-item ~{:id id
                                                                 :item-id item-id})])
                   (when onSelect (onSelect item-id)))
        open-menu (fn [evt]
                    (.stopPropagation evt)
                    (when-not disabled? (prim/transact! this
                                                        `[(close-all-dropdowns {})
                                                          (set-dropdown-open ~{:id id
                                                                               :open? (not open?)})]))
                    false)
        dropdown-attrs (merge-with attrs-merge
                                   {:id did
                                    :className (cond-> "dropdown"
                                                 open? (str " is-active"))}
                                   dropdown-attrs)
        trigger-attrs (merge-with attrs-merge {:className "dropdown-trigger"} (:trigger attrs))
        button-attrs (merge-with attrs-merge
                                 (cond-> {:id dbid
                                          :className "button"
                                          :aria-haspopup "true"
                                          :aria-controls dmid}
                                   (not is-hoverable?) (assoc :onClick open-menu))
                                 (:button attrs))
        button-label-attrs (merge-with attrs-merge
                                       (when disabled? {:className "is-disabled"})
                                       (:button-label attrs))
        button-icon-span-attrs (merge-with #(attrs-merge %1 %2 [#"\bis-small\b"])
                                           {:className "icon is-small"} (:button-icon-span attrs))
        button-icon-attrs (merge-with #(attrs-merge %1 %2 [#"fa\s+\S+"])
                                      {:className "fa fa-angle-down"
                                       :aria-hidden "true"}
                                      (:button-icon attrs))
        menu-attrs (merge-with attrs-merge
                               {:className "dropdown-menu"
                                :id dmid
                                :role "menu"}
                               (:menu attrs))
        content-attrs (merge-with attrs-merge {:className "dropdown-content"} (:content attrs))
        dropdown-items (map #(ui-dropdown-item %
                                               {:onSelect onSelect
                                                :active? (and stateful?
                                                              (= (::id %) active-item))
                                                :attrs attrs
                                                :render-fcn (when-let [render-fcn-kw (::render-fcn-kw %)]
                                                              (render-fcn-kw render-fcns))})
                            items)]
    (dom/div (clj->js dropdown-attrs)
             (dom/div (clj->js trigger-attrs)
                      (dom/button (clj->js button-attrs)
                                  (dom/span (clj->js button-label-attrs) (tr-unsafe label))
                                  (dom/span (clj->js button-icon-span-attrs)
                                            (dom/i (clj->js button-icon-attrs)))))
             (dom/div (clj->js menu-attrs)
                      (dom/div (clj->js content-attrs)
                               dropdown-items)))))

(let [ui-dropdown-factory (prim/factory Dropdown {:keyfn ::id})]
  (defn ui-dropdown
    "Render a dropdown. The props are the state props of the dropdown. The
  additional arguments are in cprops (all optional):

     onSelect - the function to call when a menu item is selected;
     stateful? - if set to true, the dropdown will remember the selection and show it;
     attrs - a map with (optional) keys containing attributes to be applied:
             :dropdown - the enclosing dropdown
             :trigger - the enclosing dropdown trigger
             :button - the button in the trigger
             :button-label - the label for the button
             :button-icon-span - the icon span (:className content replaces is-small)
             :button-icon - the icon (:className content replaces fa fa-angle-down)
             :menu - the enclosing dropdown menu
             :content - the content of the menu
             :items - the items of the dropdown
     render-fcns - to specify render functions for subitems (which can have arbitrary content)."
    [props {:keys [onSelect stateful? attrs render-fcns] :as cprops}]
    (ui-dropdown-factory (prim/computed props cprops))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; NAVBAR
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def navbar-link-table :bulma.navbar-link/by-id)
(def navbar-dropdown-table :bulma.navbar-dropdown/by-id)

(def navbar-brand-table :bulma.navbar-brand/by-id)
(def navbar-brand-item-table :bulma.navbar-brand-item/by-id)

(def navbar-menu-table :bulma.navbar-menu/by-id)

(def navbar-menu-start-table :bulma.navbar-menu-start/by-id)
(def navbar-menu-start-item-table :bulma.navbar-menu-start-item/by-id)

(def navbar-menu-end-table :bulma.navbar-menu-end/by-id)
(def navbar-menu-end-item-table :bulma.navbar-menu-end-item/by-id)

(def navbar-table :bulma.navbar/by-id)

(defn navbar-link-ident [id-or-props]
  (if (map? id-or-props)
    [(:type id-or-props) (::id id-or-props)]
    [navbar-link-table id-or-props]))

(defn navbar-dropdown-ident [id-or-props]
  (if (map? id-or-props)
    [(:type id-or-props) (::id id-or-props)]
    [navbar-dropdown-table id-or-props]))

(defn navbar-brand-ident [id-or-props]
  (if (map? id-or-props)
    (navbar-brand-ident (::id id-or-props))
    [navbar-brand-table id-or-props]))

(defn navbar-menu-ident [id-or-props]
  (if (map? id-or-props)
    (navbar-menu-ident (::id id-or-props))
    [navbar-menu-table id-or-props]))

(defn navbar-menu-start-ident [id-or-props]
  (if (map? id-or-props)
    (navbar-menu-start-ident (::id id-or-props))
    [navbar-menu-start-table id-or-props]))

(defn navbar-menu-end-ident [id-or-props]
  (if (map? id-or-props)
    (navbar-menu-end-ident (::id id-or-props))
    [navbar-menu-end-table id-or-props]))

(defn navbar-ident [id-or-props]
  (if (map? id-or-props)
    (navbar-ident (::id id-or-props))
    [navbar-table id-or-props]))

(defn navbar-link
  "Creates a navigation link. ID must be globally unique. The label will
  be run through `tr-unsafe`, so it can be internationalized.

  id - the ID for the link;
  label - the label for the link;
  displayed? - is it displayed or not? (see co-located CSS for is-not-displayed);
  stateful? - is it marked when active?;
  disabled? - is it disabled (see colocated CSS for is-disabled)."
  ([id label] (navbar-link id label {}))
  ([id label {:keys [:displayed? :stateful? :disabled?]
              :or {:displayed? true
                   :stateful? true
                   :disabled? false}}] {::id id
                                        :type navbar-link-table
                                        ::label label
                                        ::displayed? displayed?
                                        ::stateful? stateful?
                                        ::disabled? disabled?}))

(defn navbar-sublink
  "Define the state for a navbar sublink (which has arbitrary content in a div)
  of a navbar, which will be rendered by a render function,
  designated with the fcn-kw keyword. Must have a unique ID."
  [id fcn-kw] {::id id
               :type navbar-link-table
               ::label ::sublink
               ::displayed? true
               ::stateful? false
               ::disabled? false
               ::render-fcn-kw fcn-kw})

(defn navbar-divider
  "Creates a divider between navbar-dropdown links. Must have a unique ID."
  [id] {::id id
        :type navbar-link-table
        ::label ::divider
        ::displayed? true
        ::stateful? false
        ::disabled? false})

(defn navbar-dropdown
  "Creates a navigation dropdown control.
  
  id - the ID for the link;
  label - the label for the dropdown;
  displayed? - is it displayed or not? (see colocated CSS for is-not-displayed);
  stateful? - is it marked when active?;
  disabled? - is it disabled (see co-located CSS for is-not-displayed);
  links - A vector of `navbar-link` instances."
  ([id label links] (navbar-dropdown id label {} links))
  ([id label
    {:keys [:displayed? :stateful? :disabled?]
     :or {:displayed? true :stateful? true :disabled? false}}
    links] {::id id
            :type navbar-dropdown-table
            ::label label
            ::open? false
            ::displayed? displayed?
            ::stateful? stateful?
            ::disabled? disabled?
            ::links links}))

(defn navbar-brand
  "Creates a navigation brand. ID must be globally unique.

  id - the ID for the brand;
  links - A vector of `navbar-link` instances."
  [id links] {::id id
              ::links links})

(defn navbar-menu-start
  "Creates a navigation start menu. ID must be globally unique. 

  id - the ID for the start menu;
  links - A vector of `navbar-link` instances."
  [id links] {::id id
              ::links links})

(defn navbar-menu-end
  "Creates a navigation end menu ID must be globally unique.

  id - the ID for the end menu;
  links - A vector of `navbar-link` instances."
  [id links] {::id id
              ::links links})

(defn navbar-menu
  "Creates a navigation menu. ID must be globally unique.

  id - the ID for the menu;
  start - A `navbar-menu-start` instance;
  end - A `navbar-menu-end` instance."
  [id start end] {::id id
                  ::start start
                  ::end end})

(defn navbar
  "Creates a navigation control.

  brand - A `navbar-brand` instance;
  brand - A `navbar-menu` instance;
  active-link-id - Which of the nested links is --initially-- the active one;
  links - A vector of `navbar-link` or `navbar-dropdown`instances."
  [id active-link-id brand menu links] {::id id
                                        ::collapsed? true
                                        ::active-link active-link-id
                                        ::brand brand
                                        ::menu menu
                                        ::links links})

(defsc NavbarLink [this
                   {:keys [::id :type ::label
                           ::displayed? ::stateful? ::disabled?
                           ::render-fcn-kw] :as props}
                   {:keys [onSelect active-link attrs render-fcn] :as cprops}
                   {:keys [is-not-displayed is-disabled] :as css-classes}]
  {:ident (fn [] (navbar-link-ident props))
   :query [::id ::label
           ::displayed? ::stateful? ::disabled?
           ::render-fcn-kw :type]
   :initial-state (fn [_] {::id nil  ::label nil
                           ::displayed? nil ::stateful? nil ::disabled? nil ::render-fcn-kw nil})
   :css [[:.is-not-displayed {:display "none"}]
         [:.is-disabled {:text-decoration "line-through"}]]}
  (let [active? (= id active-link)
        link-attrs (merge-with attrs-merge
                               {:className (cond-> "navbar-item"
                                             active? (str " is-active")
                                             disabled? (str " " is-disabled)
                                             (not displayed?) (str " " is-not-displayed))
                                :onClick #(when onSelect (onSelect id))}
                               (:links attrs))]
    (case label
      ::divider (dom/hr #js {:key id
                             :className "navbar-divider"})
      ::sublink (dom/div #js {:key id
                              :className "navbar-item"}
                         (if render-fcn
                           (render-fcn this)
                           (report-error 501 render-fcn-kw)))
      (dom/a (clj->js link-attrs)
             (tr-unsafe label)))))

(let [ui-navbar-link-factory (prim/factory NavbarLink {:keyfn ::id})]
  (defn ui-navbar-link
    "Render a navbar link. The props are the state props of the
     navbar link. The additional arguments are in cprops:

     onSelect - the function to call when a menu item is selected;
     active-link - when this matches id then render this item as active;
     attrs - will be added to the navbar link's attributes;
     render-fcn - render function (gets NavbarLink props as a parameter)."
    [props {:keys [onSelect active-link attrs render-fcn] :as cprops}]
    (ui-navbar-link-factory (prim/computed props cprops))))

(m/defmutation set-navbar-dropdown-open
  "Mutation. Set the open? flag to true/false to open/close the dropdown."
  [{:keys [id open?]}]
  (action [{:keys [state]}]
          (let [kpath (conj (navbar-dropdown-ident id) ::open?)]
            (swap! state assoc-in kpath open?))))

(m/defmutation disable-navbar-dropdown
  "Mutation. Set the disabled? flag for a navbar-dropdown.

  id - the ID of the navbar-dropdown;
  disabled? - true or false."
  [{:keys [id disabled?]}]
  (action [{:keys [state]}]
          (swap! state update-in (navbar-dropdown-ident id) assoc ::disabled? disabled?)))

(m/defmutation display-navbar-dropdown
  "Mutation. Set the displayed? flag for a navbar-dropdown.

  id - the ID of the navbar-dropdown;
  displayed? - true or false."
  [{:keys [id displayed?]}]
  (action [{:keys [state]}]
          (swap! state update-in (navbar-dropdown-ident id) assoc ::displayed? displayed?)))

(m/defmutation set-navbar-dropdown-active-link
  "Mutation. Set active-link to the indicated link of the dropdown.

  id - the ID of the dropdown;
  target - the ID of the dropdown link."
  [{:keys [id target]}]
  (action [{:keys [state]}]
          (swap! state update-in (navbar-dropdown-ident id) assoc ::active-link target)))

(defn- close-all-navbar-dropdowns-impl [dropdown-map]
  (reduce (fn [m id] (assoc-in m [id ::open?] false)) dropdown-map (keys dropdown-map)))

(m/defmutation close-all-navbar-dropdowns
  "Mutations: Close all dropdowns (globally)"
  [ignored]
  (action [{:keys [state]}]
          (swap! state update navbar-dropdown-table close-all-navbar-dropdowns-impl)))

(defsc NavbarDropdown [this
                       {:keys [::id :type ::label ::open?
                               ::displayed? ::stateful? ::disabled?
                               ::links] :as props}
                       {:keys [onSelect active-link attrs render-fcns] :as cprops}
                       {:keys [is-not-displayed is-disabled] :as css-classes}]
  {:ident (fn [] (navbar-dropdown-ident props))
   :query [::id :type ::label ::open?
           ::displayed? ::stateful? ::disabled?
           {::links (prim/get-query NavbarLink)}]
   :initial-state (fn [_] {::id nil ::type nil ::label nil ::open? nil
                           ::displayed? nil ::stateful? nil ::disabled? nil ::links nil})
   :css-include [NavbarLink]
   :css [[:.is-not-displayed {:display "none"}]
         [:.is-disabled {:text-decoration "line-through"}]]}
  (let [dcid (str "navbar-dropdown-container-" (name id))
        dlid (str "navbar-link-" (name id))
        did (str "navbar-dropdown-" (name id))
        dropdown-container-attrs (:dropdown-container attrs)
        is-hoverable? (and dropdown-container-attrs
                           (not (str/blank? (:className dropdown-container-attrs)))
                           (str/includes? (str/lower-case (:className dropdown-container-attrs))
                                          "is-hoverable"))
        onSelect (fn [link-id]
                   (prim/transact! this
                                   `[(close-all-navbar-dropdowns {})
                                     (set-navbar-dropdown-active-link ~{:id id
                                                                        :target link-id})])
                   (when onSelect (onSelect link-id)))
        open-menu (fn [evt]
                    (.stopPropagation evt)
                    (when (and displayed? (not disabled?))
                      (prim/transact! this
                                      `[(close-all-navbar-dropdowns {})
                                        (set-navbar-dropdown-open ~{:id id
                                                                    :open? (not open?)})]))
                    false)
        dropdown-container-attrs (merge-with attrs-merge
                                             {:id dcid
                                              :className (cond-> "navbar-item has-dropdown"
                                                           open? (str " is-active")
                                                           disabled? (str " " is-disabled)
                                                           (not displayed?) (str " " is-not-displayed))}
                                             dropdown-container-attrs)
        dropdown-link-attrs (merge-with attrs-merge
                                        (cond-> {:id dlid
                                                 :className "navbar-link"}
                                          (not is-hoverable?) (assoc :onClick open-menu))
                                        (:dropdown-link attrs))
        dropdown-attrs (merge-with attrs-merge
                                   {:id did
                                    :className "navbar-dropdown"}
                                   (:dropdown attrs))
        dropdown-links (map #(ui-navbar-link %
                                             {:onSelect onSelect
                                              :active-link active-link
                                              :attrs (:links attrs)
                                              :render-fcn (when-let [render-fcn-kw (::render-fcn-kw %)]
                                                            (render-fcn-kw render-fcns))})
                            links)]
    (dom/div (clj->js dropdown-container-attrs)
             (dom/a (clj->js dropdown-link-attrs) (tr-unsafe label))
             (dom/div (clj->js dropdown-attrs)
                      dropdown-links))))

(let [ui-navbar-dropdown-factory (prim/factory NavbarDropdown {:keyfn ::id})]
  (defn ui-navbar-dropdown
    "Render a navbar-dropdown. The props are the state props of the
  navbar-dropdown. The additional arguments are in cprops (all
  optional):

     onSelect - the function to call when a menu item is selected;
     attrs - a map with (optional) keys containing attributes to be applied:
             :dropdown-container
             :dropdown-link
             :dropdown
             :links."
    [props {:keys [onSelect active-link attrs render-fcns] :as cprops}]
    (ui-navbar-dropdown-factory (prim/computed props cprops))))

(defsc NavbarItemUnion [this
                        {:keys [::id :type] :as props}
                        {:keys [onSelect active-link attrs render-fcns] :as cprops}]
  {:ident (fn [] [type id])
   :query (fn [] {navbar-link-table (prim/get-query NavbarLink)
                  navbar-dropdown-table (prim/get-query NavbarDropdown)})
   :css-include [NavbarLink]}
  (let [params  {:onSelect onSelect
                 :active-link active-link
                 :attrs attrs}]
    ;; Beware: test-constants are not evaluated. They must be compile-time literals, and need not be quoted.
    (case type
      ;; navbar-link-table
      :bulma.navbar-link/by-id (let [render-fcn-value (when-let [render-fcn-kw (::render-fcn-kw props)]
                                                        (render-fcn-kw render-fcns))
                                     render-params {:render-fcn render-fcn-value}]
                                 (ui-navbar-link props
                                                 (merge params
                                                        render-params)))
      ;; navbar-dropdown-table
      :bulma.navbar-dropdown/by-id (ui-navbar-dropdown props
                                                       (merge params
                                                              {:render-fcns render-fcns}))
      (report-error 500 "Unknown link type!"))))

(let [ui-navbar-item-factory (prim/factory NavbarItemUnion {:keyfn ::id})]
  (defn ui-navbar-item
    "Factory for component NavbarItemUnion. The props are the
  state props of the navbar-item. The additional arguments are in
  cprops (all optional):

     onSelect - the function to call when a menu item is selected;
     active-link - the ID of the active link (which be nested in a dropdown);
     attrs - various attributes;
     render-fcns - to specify render functions for sublinks (which can have arbitrary content)."
    [props {:keys [onSelect active-link attrs render-fcns] :as cprops}]
    (ui-navbar-item-factory (prim/computed props cprops))))

(defsc NavbarBrand [this
                    {:keys [::id ::links] :as props}
                    {:keys [onSelect onClick collapsed? active-link attrs render-fcns] :as cprops}]
  {:query [::id
           {::links (prim/get-query NavbarItemUnion)}]
   :ident (fn [] (navbar-brand-ident props))
   :css-include [NavbarItemUnion]}
  (let [brand-attrs (merge-with attrs-merge
                                {:className "navbar-brand"}
                                (:brand attrs))
        brand-url-attrs (merge {:href "/"}
                               (:brand-url attrs))
        brand-img-attrs (:brand-img attrs)]
    (dom/div (clj->js brand-attrs)
             (dom/a #js {:className "navbar-item"
                         :href (:href brand-url-attrs)}
                    (dom/img (clj->js brand-img-attrs)))
             (map #(ui-navbar-item %
                                   {:onSelect onSelect
                                    :active-link active-link
                                    :attrs attrs
                                    :render-fcns render-fcns})
                  links)
             (dom/button #js {:className (cond-> "button navbar-burger"
                                           (not collapsed?) (str " is-active"))
                              :onClick onClick}
                         (dom/span nil)
                         (dom/span nil)
                         (dom/span nil)))))

(let [ui-navbar-brand-factory (prim/factory NavbarBrand {:keyfn ::id})]
  (defn ui-navbar-brand
    "Render a navbar brand. The props are the state props of the
     navbar brand. The additional arguments are in cprops:

     onSelect - the function to call when a brand's links is selected;
     attrs - will be added to the navbar brand's attributes."
    [props {:keys [onSelect onClick collapsed? active-link attrs] :as cprops}]
    (ui-navbar-brand-factory (prim/computed props cprops))))

(defsc NavbarMenuStart [this
                        {:keys [::id ::links] :as props}
                        {:keys [onSelect active-link attrs render-fcns] :as cprops}]
  {:query [::id
           {::links (prim/get-query NavbarItemUnion)}]
   :ident (fn [] (navbar-menu-start-ident props))
   :css-include [NavbarItemUnion]}
  (let [start-attrs (merge-with attrs-merge
                                {:className "navbar-start"}
                                (:start attrs))]
    (dom/div (clj->js start-attrs)
             (map #(ui-navbar-item %
                                   {:onSelect onSelect
                                    :active-link active-link
                                    :attrs attrs
                                    :render-fcns render-fcns})
                  links))))

(let [ui-navbar-menu-start-factory (prim/factory NavbarMenuStart {:keyfn ::id})]
  (defn ui-navbar-menu-start
    "Render a navbar menu start. The props are the state props of the
     navbar menu start. The additional arguments are in cprops:

     onSelect - the function to call when a menu start's items is selected;
     attrs - will be added to the navbar menu start's attributes."
    [props {:keys [onSelect active-link attrs] :as cprops}]
    (ui-navbar-menu-start-factory (prim/computed props cprops))))

(defsc NavbarMenuEnd [this
                      {:keys [::id ::links] :as props}
                      {:keys [onSelect active-link attrs render-fcns] :as cprops}]
  {:query [::id
           {::links (prim/get-query NavbarItemUnion)}]
   :ident (fn [] (navbar-menu-end-ident props))
   :css-include [NavbarItemUnion]}
  (let [end-attrs (merge-with attrs-merge
                              {:className "navbar-end"}
                              (:end attrs))]
    (dom/div (clj->js end-attrs)
             (map #(ui-navbar-item %
                                   {:onSelect onSelect
                                    :active-link active-link
                                    :attrs attrs
                                    :render-fcns render-fcns})
                  links))))

(let [ui-navbar-menu-end-factory (prim/factory NavbarMenuEnd {:keyfn ::id})]
  (defn ui-navbar-menu-end
    "Render a navbar menu end. The props are the state props of the
     navbar menu end. The additional arguments are in cprops:

     onSelect - the function to call when a menu end's items is selected;
     attrs - will be added to the navbar menu end's attributes."
    [props {:keys [onSelect active-link attrs] :as cprops}]
    (ui-navbar-menu-end-factory (prim/computed props cprops))))

(defsc NavbarMenu [this
                   {:keys [::id ::start ::end] :as props}
                   {:keys [onSelect collapsed? active-link attrs render-fcns] :as cprops}]
  {:query [::id
           {::start (prim/get-query NavbarMenuStart)}
           {::end (prim/get-query NavbarMenuEnd)}]
   :ident (fn [] (navbar-menu-ident props))}
  (let [menu-attrs (merge-with attrs-merge
                               {:className (cond-> "navbar-menu"
                                             (not collapsed?) (str " is-active"))}
                               (:menu attrs))]
    (dom/div (clj->js menu-attrs)
             (when start
               (ui-navbar-menu-start start
                                     {:onSelect onSelect
                                      :active-link active-link
                                      :attrs attrs
                                      :render-fcns render-fcns}))
             (when end
               (ui-navbar-menu-end end
                                   {:onSelect onSelect
                                    :active-link active-link
                                    :attrs attrs
                                    :render-fcns render-fcns})))))

(let [ui-navbar-menu-factory (prim/factory NavbarMenu {:keyfn ::id})]
  (defn ui-navbar-menu
    "Render a navbar menu. The props are the state props of the
     navbar menu. The additional arguments are in cprops:

     onSelect - the function to call when a menu's start or end items is selected;
     attrs - will be added to the navbar menu's attributes."
    [props {:keys [onSelect collapsed? active-link attrs render-fcns] :as cprops}]
    (ui-navbar-menu-factory (prim/computed props cprops))))

(defn set-active-navbar-link*
  [state-map navbar-id target]
  (let [stateful? (-> state-map
                      (get-in (navbar-link-ident target))
                      ::stateful?)]
    (if stateful?
      (update-in state-map (navbar-ident navbar-id) assoc ::active-link target)
      state-map)))

(m/defmutation set-active-navbar-link
  "Mutation: Set the active navigation link"
  [{:keys [id target]}]
  (action [{:keys [state]}]
          (swap! state set-active-navbar-link* id target)))

(m/defmutation disable-navbar-link
  "Mutation. Set the disabled? flag for one of the navbar-links.

  target - the ID of the navbar-link;
  disabled? - true or false."
  [{:keys [target disabled?]}]
  (action [{:keys [state]}]
          (swap! state update-in (navbar-link-ident target) assoc ::disabled? disabled?)))

(m/defmutation display-navbar-link
  "Mutation. Set the displayed? flag for one of the navbar-links.

  target - the ID of the navbar-link;
  displayed? - true or false."
  [{:keys [target displayed?]}]
  (action [{:keys [state]}]
          (swap! state update-in (navbar-link-ident target) assoc ::displayed? displayed?)))

(m/defmutation toggle-navbar-collapse
  "Mutation. Toggle collapse of a dropdown's navbar-burger menu.

  id - the ID of the dropdown."
  [{:keys [id]}]
  (action [{:keys [state]}]
          (swap! state update-in (navbar-ident id) update ::collapsed? not)))

(defsc Navbar [this
               {:keys [::id ::active-link ::collapsed? ::brand ::menu ::links] :as props}
               {:keys [onSelect attrs render-fcns] :as cprops}]
  {:query [::id
           ::collapsed?
           ::active-link
           {::brand (prim/get-query NavbarBrand)}
           {::menu (prim/get-query NavbarMenu)}
           {::links (prim/get-query NavbarItemUnion)}]
   :ident (fn [] (navbar-ident props))
   :css-include [NavbarItemUnion]}
  (let [navbar-attrs (merge-with attrs-merge
                                 {:className "navbar"
                                  :role "navigation"
                                  :aria-label "main navigation"}
                                 (:navbar attrs))
        onSelect (fn [navbar-id]
                   (when onSelect (onSelect navbar-id))
                   (prim/transact! this
                                   `[(close-all-navbar-dropdowns {})
                                     (set-active-navbar-link ~{:id id :target navbar-id})]))
        onClick (fn [] (prim/transact! this
                                       `[(toggle-navbar-collapse ~{:id id})]))
        navbar-links (map #(ui-navbar-item % {:onSelect onSelect
                                              :active-link active-link
                                              :attrs attrs
                                              :render-fcns render-fcns})
                          links)
        navbar-brand (ui-navbar-brand brand {:onSelect onSelect
                                             :onClick onClick
                                             :collapsed? collapsed?
                                             :active-link active-link
                                             :attrs attrs
                                             :render-fcns render-fcns})
        navbar-menu (ui-navbar-menu menu {:onSelect onSelect
                                          :collapsed? collapsed?
                                          :active-link active-link
                                          :attrs attrs
                                          :render-fcns render-fcns})]
    (dom/div (clj->js navbar-attrs)
             (when links navbar-links)
             (when brand navbar-brand)
             (when menu navbar-menu))))

(let [navbar-factory (prim/factory Navbar {:keyfn ::id})]
  (defn ui-navbar
    "Render a nav, which should have state declared with `nav` in props.

    props - a cljs map of the data props

    The additional arguments are in cprops (all optional):

    onSelect - an optional named parameter to supply a function that is called when navigation is done;
    attrs - a map with (optional) keys containing attributes to be applied:
             :navbar - the enclosing navbar
             :brand - the enclosing navbar brand
             :brand-url - the URL of the brand
             :brand-img - the image of the brand
             :menu - the enclosing navbar menu
             :start - the enclosing navbar menu start
             :end - the enclosing navbar menu end
             :links - applied to the links (all levels) of the navbar
             :dropdown-container - the enclosing container for a navbar dropdown
             :dropdown-link - the 'open' link of a dropdown
             :dropdown - the enclosing dropdown (in which there are links)
    render-fcns - to specify render functions for subitems (which can have arbitrary content)."
    [props {:keys [onSelect attrs render-fcns] :as cprops}]
    (navbar-factory (prim/computed props cprops))))

(defn bulma-ssr-css
  "Returns the (initial) co-located Bulma CSS for Server Side Rendering."
  []
  (let [not-displayed-css "{display: none;}"
        disabled-css "{text-decoration: line-through;}"
        css-class-names-navbar-link (css/get-classnames NavbarLink)
        css-class-names-navbar-dropdown (css/get-classnames NavbarDropdown)]
    (str "<style>"
         "." (:is-not-displayed css-class-names-navbar-link) " " not-displayed-css
         "." (:is-disabled css-class-names-navbar-link) " " disabled-css
         "." (:is-not-displayed css-class-names-navbar-dropdown) " " not-displayed-css
         "." (:is-disabled css-class-names-navbar-dropdown) " " disabled-css
         "</style>")))
