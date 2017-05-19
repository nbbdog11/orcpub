(ns orcpub.dnd.e5.modifiers
  (:require [clojure.spec :as spec]
            [clojure.string :as s]
            [orcpub.common :as common]
            [orcpub.modifiers :as mods]
            [orcpub.entity-spec :as es]
            [orcpub.dnd.e5.character :as char5e]
            [orcpub.dnd.e5.skills :as skill5e])
  #?(:cljs (:require-macros [orcpub.entity-spec :as es]
                            [orcpub.modifiers :as mods])))

(defn al-illegal [reason]
  (mods/set-mod ?al-illegal-reasons reason))

(defn used-resource [resource option-name]
  (mods/set-mod ?used-resources {:resource-key resource
                                 :option-name option-name}))

(defn cls [cls-key]
  (mods/vec-mod ?classes (if ((set ?classes) cls-key)
                            ?classes
                            (conj ?classes cls-key))))

(defn subclass [cls-key subclass-key]
  (mods/modifier ?levels (assoc-in ?levels [cls-key :subclass] subclass-key)))

(defn alignment [alignment]
  (mods/modifier ?alignment alignment))

(defn race [nm]
  (mods/modifier ?race nm))

(defn background [nm]
  (mods/modifier ?background nm))

(defn subrace [nm]
  (mods/modifier ?subrace nm))

(defn resistance-cfg [value qualifier]
  {:value value
   :qualifier qualifier})

(defn damage-resistance [value & [qualifier-text]]
  (mods/set-mod ?damage-resistances (resistance-cfg value qualifier-text) (name value) "damage resistance"))

(defn damage-immunity [value & [qualifier-text]]
  (mods/set-mod ?damage-immunities (resistance-cfg value qualifier-text) (name value) "damage immunity"))

(defn immunity [value & [qualifier-text]]
  (mods/set-mod ?immunities (resistance-cfg value qualifier-text) (name value) "immunity"))

(defn condition-immunity [value & [qualifier-text]]
  (mods/set-mod ?condition-immunities (resistance-cfg value qualifier-text) (name value) " condition immunity"))

(defn darkvision [value & [order-number]]
  (mods/modifier
   ?darkvision
   value
   "Darkvision"
   (str value " feet")
   nil
   order-number))

(defn darkvision-bonus [value]
  (mods/cum-sum-mod
   ?darkvision-bonus
   value
   "Darkvision"
   (common/bonus-str value)))

(defn speed [value]
  (mods/cum-sum-mod ?speed value "speed" (mods/bonus-str value)))

(defn speed-override [value]
  (mods/vec-mod ?speed-overrides value))

(defn flying-speed [value]
  (mods/modifier ?flying-speed value "flying" (mods/bonus-str value)))

(defn flying-speed-equal-to-walking []
  (mods/modifier ?flying-speed (if (and (int? ?flying-speed)
                                        (> ?flying-speed ?speed))
                                 ?flying-speed
                                 ?speed)))

(defn swimming-speed [value]
  (mods/cum-sum-mod ?swimming-speed value "swimming" (mods/bonus-str value)))

(defn climbing-speed [value]
  (mods/cum-sum-mod ?climbing-speed value "climbing" (mods/bonus-str value)))

(defn unarmored-speed-bonus [value]
  (mods/cum-sum-mod ?unarmored-speed-bonus value "unarmored speed" (mods/bonus-str value)))

(defn ability [ability bonus]
  (mods/modifier ?ability-increases
                 (update ?ability-increases ability + bonus)
                 (clojure.string/upper-case (name ability))
                 (mods/bonus-str bonus)))

(defn conditional-ability [ability bonus conditions]
  (mods/modifier ?ability-increases
                   (update ?ability-increases ability + bonus)
                   (clojure.string/upper-case (name ability))
                   (mods/bonus-str bonus)
                   (vec conditions)))

(defn ability-override [ability value]
  (mods/vec-mod ?ability-overrides {:ability ability :value value}))

(defn level-ability-increase [ability bonus]
  (mods/modifier ?level-ability-increases
                 (update ?level-ability-increases ability + bonus)
                 (clojure.string/upper-case (name ability))
                 (mods/bonus-str bonus)))

(defn race-ability [ability-kw bonus]
  [(ability ability-kw bonus)
   (mods/modifier ?race-ability-increases
                  (update ?race-ability-increases ability-kw + bonus))])

(defn subrace-ability [ability-kw bonus]
  [(ability ability-kw bonus)
   (mods/modifier ?subrace-ability-increases
                  (update ?subrace-ability-increases ability-kw + bonus))])

(defn abilities [abilities]
  (mods/modifier ?base-abilities abilities))

(defn deferred-abilities []
  (mods/deferred-modifier
    ?abilities
    (fn [abils]
      (abilities abils))
    {:str 12 :dex 12 :con 12 :int 12 :wis 12 :cha 12}))

(defn deferred-ability-increases []
  (mods/deferred-modifier
    ?ability-increases
    (fn [increases]
      (mods/modifier ?ability-increases (merge-with + increases ?ability-increases)))
    {:str 0 :dex 0 :con 0 :int 0 :wis 0 :cha 0}))

(defn saving-throws [cls-kw & abilities]
  (mods/modifier ?saving-throws
                 (apply conj (or ?saving-throws #{}) abilities)
                 nil;;"Saving Throws"
                 nil;;(s/join ", " (map (comp s/upper-case name) abilities))
                 [(or (nil? cls-kw) (= cls-kw (first ?classes)))]))

(defn saving-throw-bonus [ability-kw bonus]
  (mods/modifier ?saving-throw-bonuses
                 (update ?saving-throw-bonuses ability-kw + bonus)))

(defn saving-throw-bonuses [bonus]
  (mods/modifier ?saving-throw-bonuses
                 (merge-with +
                             ?saving-throw-bonuses
                             (zipmap [:str :con :dex :int :wis :cha] (repeat bonus)))))

(defn saving-throw-advantage [types & [abilities]]
  (mods/vec-mod ?saving-throw-advantage {:abilities abilities
                                         :types types}))

(defn initiative [bonus]
  (mods/cum-sum-mod ?initiative bonus "Initiative" (mods/bonus-str bonus)))

(defn level [class-key class-nm level hit-die]
  (mods/modifier ?levels (update ?levels class-key merge {:class-name class-nm
                                                          :class-level level
                                                          :hit-die hit-die})))

(defn enough-levels? [class-key total-levels class-level level]
  (or (nil? level)
      (>= (if class-key
            (class-level class-key)
            total-levels)
          level)))

(defmacro spells-known-cfg [level {class-key :class-key :as spell-cfg} min-level conditions]
  `(mods/modifier
    ~'?spells-known
    (if (enough-levels? ~class-key ~'?total-levels ~'?class-level ~min-level)
      (update
       ~'?spells-known
       ~level
       conj
       ~spell-cfg)
      ~'?spells-known)
    nil
    nil
    ~conditions))

(defn spell-data [spell-key spellcasting-ability qualifier class]
  {:key spell-key
   :ability spellcasting-ability
   :qualifier qualifier
   :class class})

(defn spells-known [level spell-key spellcasting-ability class & [min-level qualifier class-key]]
  (mods/modifier
    ?spells-known
    (if (enough-levels? class-key ?total-levels ?class-level min-level)
      (update
       ?spells-known
       level
       conj
       (spell-data
        spell-key
        spellcasting-ability
        qualifier
        class))
      ?spells-known)))

(defn spell-slot-factor [class-key factor]
  (mods/map-mod ?spell-slot-factors class-key factor))

(defn spell-save-dc-bonus [bonus]
  (mods/cum-sum-mod ?spell-save-dc-bonus bonus))

(defn spell-attack-modifier-bonus [bonus]
  (mods/cum-sum-mod ?spell-attack-modifier-bonus bonus))

(defn trait-cfg [{:keys [name description class-key level summary page conditions source] :as cfg}]
  (let [class-key? (not (nil? class-key))]
    (mods/modifier ?traits
                   (if (or (nil? level)
                           (>= (if class-key?
                                 (?class-level class-key)
                                 ?total-levels)
                               level))
                     (conj
                      ?traits
                      cfg)
                     ?traits))))

(defn trait [name & [description level summary conditions]]
  (trait-cfg {:name name :description description :level level :summary summary :conditions conditions}))

(defmacro prop-trait [prop {:keys [level conditions class-key] :or {level 1} :as t}]
  (let [all-conditions (conj conditions `(enough-levels? ~class-key ~'?total-levels ~'?class-level ~level))]
    `(mods/modifier
      ~prop
      (conj ~prop ~t)
      nil
      nil
      ~all-conditions)))

(defmacro dependent-trait [t]
  `(prop-trait ~'?traits ~t))

#_(prop-trait
 ?traits
 {:name "Juancho"
  :level 2
  :class-key :fighter
  :summary (str "X " ?total-levels)})

(defmacro dependent-trait-2 [{:keys [level conditions class-key] :or {level 1} :as t}]
  `(mods/modifier ~'?traits
                  (if (enough-levels? ~class-key ~'?total-levels ~'?class-level ~level)
                    (conj
                     ~'?traits
                     ~t)
                    ~'?traits)
                  nil
                  nil
                  ~conditions))

(defn proficiency-bonus [bonus]
  (mods/modifier ?proficiency-bonus bonus))

(defn skill-proficiency [skill-kw]
  (mods/set-mod ?skill-profs skill-kw (s/capitalize (-> skill-kw skill5e/skills-map :name)) "proficiency"))

(defn skill-bonus [skill-kw bonus]
  (mods/modifier ?additional-skill-bonuses
                 (update ?additional-skill-bonuses skill-kw + bonus)))

(defn max-hit-points [bonus]
  (mods/cum-sum-mod ?hit-point-level-increases bonus "HP" (mods/bonus-str bonus)))

(defn deferred-max-hit-points []
  (mods/deferred-modifier
    ?hit-point-level-increases
    max-hit-points
    1
    "HP"
    mods/bonus-str))

(defn skill-expertise [key]
  (mods/set-mod ?skill-expertise key))

(defn tool-proficiency [key & [first-class? cls-kw]]
  (if first-class?
    (mods/set-mod ?tool-profs
                  key
                  nil
                  nil
                  [(= cls-kw (first ?classes))])
    (mods/set-mod ?tool-profs
                  key)))

(defn language [key]
  (mods/set-mod ?languages key))

(defn weapon-proficiency [key & [first-class? cls-kw]]
  (if first-class?
    (mods/set-mod ?weapon-profs
                  key
                  nil
                  nil
                  [(= cls-kw (first ?classes))])
    (mods/set-mod ?weapon-profs
                  key)))

(defn armor-proficiency [key & [first-class? cls-kw]]
  (if first-class?
    (mods/set-mod ?armor-profs
                  key
                  nil
                  nil
                  [(= cls-kw (first ?classes))])
    (mods/set-mod ?armor-profs
                  key)))

(defn light-armor-proficiency [& [first-class? cls-kw]]
  (armor-proficiency :light first-class? cls-kw))

(defn medium-armor-proficiency [& [first-class? cls-kw]]
  (armor-proficiency :medium first-class? cls-kw))

(defn heavy-armor-proficiency [& [first-class? cls-kw]]
  (armor-proficiency :heavy first-class? cls-kw))

(defn shield-armor-proficiency [& [first-class? cls-kw]]
  (armor-proficiency :shields first-class? cls-kw))

(defn passive-perception [bonus]
  (mods/cum-sum-mod ?passive-perception bonus))

(defn passive-investigation [bonus]
  (mods/cum-sum-mod ?passive-investigation bonus))

(defn size [size]
  (mods/modifier ?size size))

(defn equipment-cfg [cfg]
  (if (int? cfg) {:quantity cfg :equipped? true} cfg))

(defn weapon [weapon-kw cfg]
  (mods/map-mod ?weapons weapon-kw (equipment-cfg cfg)))

(defn magic-weapon [weapon-kw cfg]
  (mods/map-mod ?magic-weapons weapon-kw (equipment-cfg cfg)))

(defn deferred-weapon [weapon-kw weapon]
  (mods/deferred-modifier
    ?weapons
    (fn [cfg] (mods/map-mod ?weapons weapon-kw (equipment-cfg cfg)))
    1))

(defn deferred-magic-item-fn [equipment-mod-fn {:keys [magical-ac-bonus modifiers]} & [include-magic-bonus?]]
  (fn [cfg] (let [equipment-mod (equipment-mod-fn cfg)]
              (if (:equipped? cfg)
                (let [mods (concat [equipment-mod]
                                   (if (and include-magic-bonus? magical-ac-bonus)
                                     [(mods/cum-sum-mod ?magical-ac-bonus magical-ac-bonus)])
                                   modifiers)]
                  mods)
                equipment-mod))))

(defn deferred-magic-weapon [weapon-kw {:keys [magical-ac-bonus modifiers] :as weapon}]
  (mods/deferred-modifier
    ?magic-weapons
    (deferred-magic-item-fn #(mods/map-mod ?magic-weapons weapon-kw (equipment-cfg %)) weapon)
    1))

(defn armor [armor-kw cfg]
  (mods/map-mod ?armor armor-kw (equipment-cfg cfg)))

(defn deferred-armor [armor-kw armor]
  (mods/deferred-modifier
    ?armor
    (fn [cfg] (mods/map-mod ?armor armor-kw (equipment-cfg cfg)))
    1))

(defn deferred-magic-armor [armor-kw armor]
  (mods/deferred-modifier
    ?armor
    (deferred-magic-item-fn (fn [cfg] (mods/map-mod ?magic-armor armor-kw (equipment-cfg cfg))) armor)
    1))

(defn equipment [equipment-kw cfg]
  (mods/map-mod ?equipment equipment-kw (equipment-cfg cfg)))

(defn deferred-equipment [equipment-kw equipment]
  (mods/deferred-modifier
    ?equipment
    (fn [cfg] (mods/map-mod ?equipment equipment-kw (equipment-cfg cfg)))
    1))

(defn deferred-treasure [treasure-kw treasure]
  (mods/deferred-modifier
    ?treasure
    (fn [cfg] (mods/map-mod ?treasure treasure-kw (equipment-cfg cfg)))
    1))

(defn deferred-magic-item [item-kw item]
  (mods/deferred-modifier
    ?magic-items
    (deferred-magic-item-fn (fn [cfg] (mods/map-mod ?magic-items item-kw (equipment-cfg cfg))) item true)
    1
    "MAGIC ITEM"))

(defn extra-attack []
  (mods/cum-sum-mod ?num-attacks 1))

(defn ranged-attack-bonus [bonus]
  (mods/cum-sum-mod ?ranged-attack-bonus bonus))

(defn armored-ac-bonus [bonus]
  (mods/cum-sum-mod ?armored-ac-bonus bonus))

(defn unarmored-ac-bonus [bonus]
  (mods/cum-sum-mod ?unarmored-ac-bonus bonus))

(defn natural-ac-bonus [bonus]
  (mods/cum-sum-mod ?natural-ac-bonus bonus))

(defmacro ac-bonus-fn [bonus-fn]
  `(mods/vec-mod ~'?ac-bonus-fns ~bonus-fn))

(defn unarmored-defense [cls]
  (mods/vec-mod ?unarmored-defense cls))

(defmacro attack [atk]
  `(mods/modifier ~'?attacks
                  (conj
                   ~'?attacks
                   ~atk)))

(defn critical [roll-value]
  (mods/set-mod ?critical roll-value))


(defmacro action [action]
  `(mods/modifier ~'?actions
                  (conj
                   ~'?actions
                   ~action)))

(defmacro bonus-action [action]
  `(prop-trait ~'?bonus-actions ~action))

(defmacro reaction [action]
  `(prop-trait ~'?reactions ~action))

(defmacro level-val [level mappings]
  (let [flat-mappings (conj (vec (apply concat (sort-by first > (dissoc mappings :default)))) (:default mappings))]
    `(condp <= ~level ~@flat-mappings)))
