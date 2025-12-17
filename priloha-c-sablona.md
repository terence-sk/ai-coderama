# AI Workflow Dokumentácia

**Meno:** Martin Švoňava

**Dátum začiatku:** 12.12.2025

**Dátum dokončenia:** 14.12.2025

**Zadanie:** Backend

---

## 1. Použité AI Nástroje

Vyplň približný čas strávený s každým nástrojom:

- [ ] **Cursor IDE:** _____ hodín
- [ ] **Claude Code:** 5 hodín  
- [ ] **GitHub Copilot:** _____ hodín
- [ ] **ChatGPT:** _____ hodín
- [ ] **Claude.ai:** _____ hodín
- [ ] **Iné:** Junie 0.5h

**Celkový čas vývoja (priližne):** _____ hodín

---

## 2. Zbierka Promptov

> 💡 **Tip:** Kopíruj presný text promptu! Priebežne dopĺňaj po každej feature.

### Prompt #1: Vytvorenie planu podla prvej casti zadania

**Nástroj:** Claude Code
**Kontext:** Prazdny springboot projekt rucne vytvoreny, do neho nakopirovane md zadania rozdelene na 2 subory

**Prompt:**
```
ultrathink about the @system-part1.md this is my first goal, if you have any questions for clarifications, feel free to ask
```

**Výsledok:**  
[x] ✅ Fungoval perfektne (first try)  
[ ] ⭐⭐⭐⭐ Dobré, potreboval malé úpravy  
[ ] ⭐⭐⭐ OK, potreboval viac úprav  
[ ] ⭐⭐ Slabé, musel som veľa prepísať  
[ ] ❌ Nefungoval, musel som celé prepísať

**Čo som musel upraviť / opraviť:**
```
Prompt bol v poriadku, akurat na konci tvorby planu sa spytal ci chcem hned kodit alebo robit nieco ine - napisal som mu nech ulozi ten plan do md suboru, ze kodit budeme neskor. To nedokazal, pretoze stale bol v plan mode, ale hned navrhol ze ak chcem, nech mu napisem nech opustu plan mode a on to urobi.
```

**Poznámky / Learnings:**
```
Spýtal sa, tak ako som mu povedal, niekoľko otázok k výberu tehcnológií, ktoré som zodpovedal entrom v 4 bodovom formulári. Po tomto bol usage na 20% a context na 58%.
```



### Prompt #2: Zaciatok programovania

**Nástroj:** Claude Code
**Kontext:** existujuci plan vygenerovany claudom

**Prompt:**
```
 There is a 12 phase plane located in @part1-plan.md lets implement phase by phase, start with phase 1
```

**Výsledok:**  
[ ] ✅ Fungoval perfektne (first try)  
[x] ⭐⭐⭐⭐ Dobré, potreboval malé úpravy  
[ ] ⭐⭐⭐ OK, potreboval viac úprav  
[ ] ⭐⭐ Slabé, musel som veľa prepísať  
[ ] ❌ Nefungoval, musel som celé prepísať  
**Úpravy:**
```
Musel som ho upozornit aby po implementacii dalsej fazy zastavil, zapisal progress do md file, pretoze som mu povedal len start with phase 1, ale on pokracoval rovno aj na phase 2.
```

**Poznámky:**
```
Problem bol vo vagnej definicii , povedal som mu zacni s fazou 1, ale zabudol som dodat ze po kazdej faze chcem zastavit a reevaluovat stav.
Context po tomto prompte (predoslom vyclearovani) bol 43%, usage 26%.
```

### Prompt #3: Pokracovanie s dalsimi 3 fazami

**Nástroj:** Claude Code
**Kontext:** existujuci plan vygenerovany claudom, 2 fazy implementovane

**Prompt:**
```
 lets continue with 3 more phases, then I will reevaluate
```

**Výsledok:**  
[x] ✅ Fungoval perfektne (first try)  
[ ] ⭐⭐⭐⭐ Dobré, potreboval malé úpravy  
[ ] ⭐⭐⭐ OK, potreboval viac úprav  
[ ] ⭐⭐ Slabé, musel som veľa prepísať  
[ ] ❌ Nefungoval, musel som celé prepísať  
**Úpravy:**
```
Nic
```

**Poznámky:**
```
Tento krok som urobil pre to aby som zistil zostavajuci kontext, aby aj zvysny vystup bol kvalitny. Context bol 46% , po spusteni compact sa znizil na 37% 
```


### Prompt #4: Dokoncenie vsetkych ostatnych faz

**Nástroj:** Claude Code
**Kontext:** existujuci plan vygenerovany claudom, 5 faz implementovanych

**Prompt:**
```
 continue with the rest of the steps
```

**Výsledok:**  
[] ✅ Fungoval perfektne (first try)  
[x] ⭐⭐⭐⭐ Dobré, potreboval malé úpravy  
[ ] ⭐⭐⭐ OK, potreboval viac úprav  
[ ] ⭐⭐ Slabé, musel som veľa prepísať  
[ ] ❌ Nefungoval, musel som celé prepísať  
**Úpravy:**
```
JAVA Kod bol sice v poriadku preto take vysoko poizitivne hodnotenie ale testy mali bug ktory nevyriesil - upravou bolo pouzitie Junie agenta na fix a nasledne prepisanie testov
```

**Poznámky:**
```
Najvacsim problemom boli claudom generovane testy ktore nedokazal uspesne spustit a opravit, snazil sa vyuzivat docker na spustenie postgres test instancie, mal v teste chybu ktoru ale neodhalil pre zrejme prilis vela logov (chyba bola pri ukladani zavislej entity ktora nemala id parenta hoci bolo povinne), pokusal sa opravit ine veci ktore nedavali zmysel, ked sa na tom dookola krutil asi 20minut, uplne mi dosla usage.
Do buducna - Po implementovani testov claudom, si ich radsej pustit rucne, a usetrit si usage.
```


### Prompt #5: Oprava testov

**Nástroj:** Junie
**Kontext:** Vsetky fazy vygenerovane, kompletny projekt, nefunkcne testy

**Prompt:**
```
 Read the readme to get the context of a project, then, fix the tests in OrderControllerIntegrationTest, OR rewrite them to not use postgres containers but h2 in memory instead. 
```

**Výsledok:**  
[x] ✅ Fungoval perfektne (first try)  
[ ] ⭐⭐⭐⭐ Dobré, potreboval malé úpravy  
[ ] ⭐⭐⭐ OK, potreboval viac úprav  
[ ] ⭐⭐ Slabé, musel som veľa prepísať  
[ ] ❌ Nefungoval, musel som celé prepísať  
**Úpravy:**
```

```

**Poznámky:**
```
Trvalo mu to relativne dlho, asi 15minut, ale dosiel na chybu a opravil tie testy ktore som mu zadal. Po spusteni vsetkych testov dokopy este ale stale nastavali chyby
```

### Prompt #5: Prepisanie testov

**Nástroj:** Junie
**Kontext:** Vsetky fazy vygenerovane, kompletny projekt, nefunkcne testy

**Prompt:**
```
Rewrite all the tests to use h2 instead of postgres test container
```

**Výsledok:**  
[x] ✅ Fungoval perfektne (first try)  
[ ] ⭐⭐⭐⭐ Dobré, potreboval malé úpravy  
[ ] ⭐⭐⭐ OK, potreboval viac úprav  
[ ] ⭐⭐ Slabé, musel som veľa prepísať  
[ ] ❌ Nefungoval, musel som celé prepísať  
**Úpravy:**
```
Nic
```

**Poznámky:**
```
Opravil vsetky testy, vsetko funguje, spustit ich naraz nevedel pretoze vraj v danej zlozke nie su (boli tam) tak spustal po jednom.
```

### Prompt #6: Aktualizacia MD files po zmene

**Nástroj:** Claude
**Kontext:** Aktualizacia dokumentacie

**Prompt:**
```
i have changed the tests from containers to h2 in memory database, please reload your context and update the according md files 
```

**Výsledok:**  
[x] ✅ Fungoval perfektne (first try)  
[ ] ⭐⭐⭐⭐ Dobré, potreboval malé úpravy  
[ ] ⭐⭐⭐ OK, potreboval viac úprav  
[ ] ⭐⭐ Slabé, musel som veľa prepísať  
[ ] ❌ Nefungoval, musel som celé prepísať  
**Úpravy:**
```
Nic
```

**Poznámky:**
```

```

### Prompt #7: Planovanie part 2

**Nástroj:** Claude
**Kontext:** Planovanie part 2

**Prompt:**
```
ultrathink about the @system-part2.md if you have any questions for clarifications, feel free to ask, do not think about the test yet, that will be done later
```

**Výsledok:**  
[x] ✅ Fungoval perfektne (first try)  
[ ] ⭐⭐⭐⭐ Dobré, potreboval malé úpravy  
[ ] ⭐⭐⭐ OK, potreboval viac úprav  
[ ] ⭐⭐ Slabé, musel som veľa prepísať  
[ ] ❌ Nefungoval, musel som celé prepísať  
**Úpravy:**
```
Nic
```

**Poznámky:**
```
Pri dotaznik s otazkami sa opytal aj to, ci ma cron zaseknutych objednavok prechadzat len status pending alebo len processing alebo oboje pretoze upozornil
na to ze by bolo logickejsie spracuvat aj processing stav i ked je to proti instrukciam v casti Expected Flow.
```

### Prompt #8: Zapis planu do MD

**Nástroj:** Claude
**Kontext:** Planovanie part 2

**Prompt:**
```
We will be coding later, just write the plan md to a file
```

**Výsledok:**  
[] ✅ Fungoval perfektne (first try)  
[x] ⭐⭐⭐⭐ Dobré, potreboval malé úpravy  
[ ] ⭐⭐⭐ OK, potreboval viac úprav  
[ ] ⭐⭐ Slabé, musel som veľa prepísať  
[ ] ❌ Nefungoval, musel som celé prepísať  
**Úpravy:**
```
Nic
```

**Poznámky:**
```
Tu tazko povedat ci fungoval perfektne, akoze ano zapisal MD file , lenze s tym ze som dal accept edit, tak on ten edit vyuzil na to, aby okrem MD
pokracoval aj v implementacii a vsetko to co bolo v plane aj realne hned zacal vykonavat. Myslim ze to bola moja chyba tym ze som mu povolil edit
a nasledne odbehol od PC!

Opatovne mal problem s tym ze mam lokalne java 8 nastavenu v env, a opatovne to vedel opravit, robi to ale zakazdym

Naslednym manualnym testovanim som zistil ze nechodia notifikacie , poziadal som o opravu v dalsom prompte...

```

### Prompt #9: Oprava notifikacii

**Nástroj:** Claude
**Kontext:** Implementacia planu druhej casti

**Prompt:**
```
 I havent been able to catch a breakpoint in OrderCompletedHandler I think notifications/message are not going through 
```

**Výsledok:**  
[x] ✅ Fungoval perfektne (first try)  
[] ⭐⭐⭐⭐ Dobré, potreboval malé úpravy  
[ ] ⭐⭐⭐ OK, potreboval viac úprav  
[ ] ⭐⭐ Slabé, musel som veľa prepísať  
[ ] ❌ Nefungoval, musel som celé prepísať  
**Úpravy:**
```
Nic
```

**Poznámky:**
```

```


---

## 3. Problémy a Riešenia 

> 💡 **Tip:** Problémy sú cenné! Ukazujú ako riešiš problémy s AI.

### Problém #1: _________________________________

**Čo sa stalo:**
```
Po vygenerovaní testov cez @TestContainers sa pri ich spustení objavila chyba
ERROR: null value in column "order_id" of relation "order_items" violates not-null constraint
ktorá je relatívne jednoducho opraviteľná.  
```

**Prečo to vzniklo:**
```
AI sa sústredilo na iné hlášky ktoré neboli gro problému
a to boli veci súvisiace s vytvorením docker containera. Na tomto sa minulo zbytočne veľa usage.
```

**Ako som to vyriešil:**
```
Použil som JUNIE na prepísanie do jednoduchšej varianty testov. 
```

**Čo som sa naučil:**
```
Pozornejšie sledovať čo číta z logov a prečo to považuje za problém.
```

**Screenshot / Kód:** [ ] Priložený


## 4. Kľúčové Poznatky

### 4.1 Čo fungovalo výborne

**1.** 
```
Generovanie plánu a doplňujúce otázky
```

**2.** 
```
```

**3.** 
```
```

**[ Pridaj viac ak chceš ]**

---

### 4.2 Čo bolo náročné

**1.** 
```
```

**2.** 
```
```

**3.** 
```
```

---

### 4.3 Best Practices ktoré som objavil

**1.** 
```
Ak claude vygeneruje viacbodový plán, a ty chceš po každom implementovanom bode zastať, aby si si
overil koľko kontextu a usage ti ostáva,  pri nejakom 12 bodovom pláne už po povedzme polovici, sa
môže stať, že zabudne na to že má zastať. Takže ak odbehneš od PC s tým že sa implementuje bod 6
kľudne sa môžeš k PC vrátiť s tým že už je všetko hotové - best practice = vždy ho kontroluj! ber to 
ako šoférovanie keď sa hráš s mobilom - ani nevieš ako a nehoda je na svete.
```

**2.** 
```
Ak mu povolíš pripojiť sa k DB, tak len s takým userom ktorý ma read only práva.
```

**3.** 
```
Šetri usage a kontext tým, že napríklad testy síce necháš vygenerovať claude, ale pusti si ich ručne.
```


---

### 4.4 Moje Top 3 Tipy Pre Ostatných

**Tip #1:**
```
Vzdy po vytvoreni planu si skontroluj ci mas allow all edits, pretoze ak ano, a omylom mu povolis zapis,
zacne implementovat, a ak si v domneni ze zapisuje len ten plan do MD a ty odbehnes od PC, moze toho spravit ovela viac.
```

**Tip #2:**
```
Pri pouzivani MCP na dolezitejsiu databazu ako je nejaky lokalny vyvoj, daj mu take pripojenie ktore je len readonly kym
si nie dost sebavedomy mu povolit viac
```

**Tip #3:**
```
```

---

## 6. Reflexia a Závery

### 6.1 Efektivita AI nástrojov

**Ktorý nástroj bol najužitočnejší?** Claude Code

**Prečo?**
```
Agent mod ktory spravi gro prace
```

**Ktorý nástroj bol najmenej užitočný?** Junie

**Prečo?**
```
Kedze som pouzil len Junie a claude code musim povedat, ze Junie ale najma kvoli tomu ze mi prislo ze je to strasne pomale
```

---

### 6.2 Najväčšie prekvapenie
```
Junie ktore som uz hadzal do kosa po tom ako vyse 20minut vypisovalo velmi podobne hlasky, 
myslel som ze sa zacyklil a potom odrazu vyriesilo problem s testami upravou par riadkov.
S claude code som uz chvilu pracoval, preto v tomto momente uz pre mna nebol takym prekvapenim.
```

---

### 6.3 Najväčšia frustrácia
```
Ked mi dosiel usage, ktory sa da minut relativne rychlo pri intenzivnej praci.
```

---

### 6.4 Najväčší "AHA!" moment
```
Ked som prvy krat zistil ze AI nie su len chatboty z ktorych musim pracne kopirovat kody a davat im kontext popisovanim, ale ze ich viem pustit
lokalne ako agentov a kontext si spravia sami a taktiez upravy. Vtedy mi fakt doslo to o com sa hovorilo a comu som dlhsie neveril, ze praca programatora
sa zmeni z kodera na citaca a opravovaca kodu po AI.
```

---

### 6.5 Čo by som urobil inak
```
Asi by som skusil vyuzit PRP subory
```

### 6.6 Hlavný odkaz pre ostatných
```
Pouzivajte AI lebo vas nahradi niekto kto ju pouziva. AI samotna vas nenahradi (zatial).
```
