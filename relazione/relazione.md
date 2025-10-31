# Homework 2 Ingegneria dei dati 2025/2026

## File Search Engine con Elasticsearch (ricette in italiano)

### URL del progetto

Repository GitHub: https://github.com/micheleguidaa/file-search-engine

---

## Dataset

Il dataset utilizzato in questo progetto è stato scaricato da Kaggle:
- **Fonte**: [Italian Food Recipes](https://www.kaggle.com/datasets/edoardoscarpaci/italian-food-recipes)
- **Formato**: CSV (`dataset/recipe.csv`)
- **Contenuto**: Collezione di 5939 ricette italiane
- **Struttura**: Il dataset contiene 6 colonne (Nome, Categoria, Link, Persone/Pezzi, Ingredienti, Steps)
- **Campi selezionati**: Dal dataset sono stati estratti il **Nome** (utilizzato come nome del file `.txt`) e **Steps** (che rappresenta la preparazione della ricetta, utilizzata come contenuto del file)

---

## Workflow

L'obiettivo del progetto è indicizzare un corpus di ricette italiane in Elasticsearch per consentire ricerche full‑text efficienti su titolo e contenuto. Il processo è articolato in due fasi principali:

### 1. Estrazione e normalizzazione dati

**Notebook**: `converter.ipynb`

**Funzionalità**:
- Lettura del file `dataset/recipe.csv`
- Estrazione dei campi Nome e Steps per ogni ricetta
- Generazione di un file `.txt` per ciascuna ricetta nella cartella `files/`
- Normalizzazione dei nomi file (sostituzione caratteri speciali come `*` `?` `:`)
- Rimozione preventiva di eventuali file preesistenti nella cartella `files/`

**Output**: 5939 file `.txt`, uno per ogni ricetta del dataset.

### 2. Indicizzazione in Elasticsearch

**Notebook**: `elasticsearch.ipynb`

**Funzionalità**:
- Creazione (o reset se esistente) dell'indice `index_recipes`
- Definizione del mapping con analyzer italiano per i campi `title` e `content`
- Indicizzazione massiva (bulk) di tutti i file `.txt` dalla cartella `files/` tramite `helpers.bulk`, che consente di indicizzare più documenti con una singola richiesta HTTP, riducendo drasticamente il numero di round‑trip verso Elasticsearch e migliorando le prestazioni rispetto all'indicizzazione documento per documento
- Misurazione dei tempi di indicizzazione

**Ambiente di esecuzione**:

L'esperimento è stato condotto su **Mac M1 con 8 GB di RAM**.

Elasticsearch viene eseguito tramite Docker Compose (`docker/elasticsearch/docker-compose.yml`) con la seguente configurazione:
- **Modalità**: Single node (`discovery.type=single-node`)
- **Sicurezza**: Disattivata per semplicità in ambiente locale (`xpack.security.enabled=false`)
- **Porte esposte**: 
  - 9200 (HTTP REST API)
  - 9300 (transport interno)

---

## Analyzer scelti e motivazioni

Nel mapping dell’indice `index_recipes`, entrambi i campi principali sono definiti come `text` e analizzati con l’analyzer italiano predefinito di Elasticsearch:

- **title**: `analyzer: "italian"`, `search_analyzer: "italian"`  
- **content**: `analyzer: "italian"`, `search_analyzer: "italian"`

---

### Come è implementato l’analyzer italiano

L’analyzer `italian` è un **analyzer linguistico built-in** fornito da Elasticsearch e basato sul motore **Apache Lucene**.  
È progettato per gestire in modo efficace le peculiarità morfologiche e ortografiche della lingua italiana, come **elisioni**, **accenti**, **plurali** e **variazioni di genere**.

Internamente, la pipeline di analisi dell’analyzer `italian` è composta da:

```json
"tokenizer": "standard",
"filter": [
  "italian_elision",
  "lowercase",
  "italian_stop",
  "italian_stemmer"
]
```

- **Tokenizer “standard”** → suddivide il testo in token (parole) rimuovendo punteggiatura e separatori.  
- **Filtro `italian_elision`** → rimuove gli articoli e le preposizioni elise, tipiche della lingua italiana (*l’amico → amico*, *dell’uva → uva*).  
- **Filtro `lowercase`** → converte tutto in minuscolo per uniformare il testo e rendere la ricerca case-insensitive.  
- **Filtro `italian_stop`** → elimina automaticamente le **stopword** più comuni (es. *“il”*, *“di”*, *“e”*, *“allo”*, *“della”*), che non apportano informazione semantica rilevante.
- **Filtro `italian_stemmer`** → applica uno **stemming leggero (`light_italian`)** basato sull’algoritmo **Snowball**, che riduce le parole alla radice solo in modo **moderato**, mantenendo le distinzioni morfologiche più importanti.  
  Ad esempio, *mirtillo/mirtilli → mirtill*, ma **non** unifica termini con radici simili ma significati diversi, come *ciliegina* ≠ *ciliegia* o *pomodorino* ≠ *pomodoro*.  

  Questo comportamento **volutamente leggero** rappresenta un **punto di forza** nel contesto del progetto:
  - evita l’**over-stemming**, che in italiano genererebbe falsi positivi (es. trattare *pomodorino* e *pomodoro* come sinonimi, anche se indicano ingredienti diversi);
  - **preserva le sfumature semantiche** importanti nel dominio culinario, dove le varianti lessicali identificano spesso ingredienti o concetti distinti;
  - consente comunque una **normalizzazione morfologica di base** (plurali, genere, elisioni), migliorando la ricerca senza distorcere il significato.

Questa combinazione di filtri consente un’**analisi linguistica bilanciata**, capace di migliorare la ricerca in italiano senza introdurre ambiguità o perdita di significato.

- **Coerenza tra indicizzazione e ricerca** → l’uso dello stesso analyzer anche come `search_analyzer` garantisce simmetria tra fase di indicizzazione e query, aumentando la probabilità di recuperare documenti pertinenti.
---

## Numero di file indicizzati e tempi di indicizzazione

L’esperimento di indicizzazione è stato condotto mediante il notebook `elasticsearch.ipynb`, che implementa l’intera pipeline di creazione e popolamento dell’indice.  
In particolare, le operazioni principali eseguite sono le seguenti:

- **Creazione dell’indice** `index_recipes`, configurato secondo il *mapping* linguistico descritto nella sezione precedente.  
- **Indicizzazione massiva (bulk)** di tutti i documenti testuali presenti nella directory `files/`, tramite la funzione `helpers.bulk` fornita dal client ufficiale `elasticsearch-py`.  
- **Misurazione del tempo di esecuzione complessivo**, ottenuta mediante cronometria diretta in Python:  

    ```python
    start = time.time()
    ...
    end = time.time()
    elapsed = end - start
    ```

### Risultati sperimentali

| Parametro                       | Valore          |
|---------------------------------|-----------------|
| Numero di documenti indicizzati | 5 939           |
| Indice di destinazione          | `index_recipes` |
| Tempo totale di indicizzazione  | 1.65 s         |

### Discussione dei risultati

Il tempo totale di **1.65 secondi** evidenzia l’elevata efficienza dell’indicizzazione in modalità *bulk*, che consente di minimizzare il numero di richieste HTTP verso il cluster Elasticsearch e di sfruttare in modo ottimale le risorse disponibili.  

Tale valore può tuttavia variare in funzione:
- delle caratteristiche hardware della macchina ospite (CPU, I/O, memoria);
- dello stato e della configurazione del cluster Elasticsearch;
- della dimensione media dei documenti indicizzati.  

Nel complesso, i risultati confermano la **scalabilità e rapidità del processo di indicizzazione** offerto da Elasticsearch, anche su dataset di migliaia di documenti testuali di dimensione medio-piccola.

---

## Query usate per testare il sistema

Il notebook `elasticsearch.ipynb` implementa un'interfaccia interattiva basata su due funzioni principali: `parse_and_search()` traduce query in linguaggio naturale (es. `tiramisu`, `title: arancini`, `"burro e salvia"`) in query Elasticsearch strutturate (`match`, `match_phrase`, `multi_match`); `search()` esegue la ricerca e formatta i risultati con highlighting automatico colorato a terminale.

### Tipologie di query supportate

1. **`match`** – Ricerca lessicale con analisi morfologica e stemming  
   Applica l'analyzer italiano configurato, consentendo match anche su varianti morfologiche (plurali, genere, forme verbali).

2. **`match_phrase`** – Ricerca di frase esatta  
   Richiede che i termini compaiano nell'ordine specificato, preservando la sequenza lessicale originale.

### Formato delle query

L'interfaccia accetta query nei seguenti formati:

**Ricerca su entrambi i campi** (senza specificare il campo):
```
termine_o_frase
```
Esempio: `tiramisu` → cerca sia in `title` che in `content`

**Ricerca field-specific**:
```
campo: termine_o_frase
```
Esempio: `title: carbonara` → cerca solo nel campo `title`

Dove:
- **campo** può essere `title` o `content`
- **termine_o_frase** può essere:
  - una o più parole (per query `match`)
  - una frase racchiusa tra virgolette `"..."` (per query `match_phrase`)

### Esempi di query utilizzate nei test

Per validare il comportamento dell'analyzer italiano e delle sue componenti nella pipeline di analisi, sono state formulate ed eseguite **10 query di test**, ciascuna progettata per verificare specifiche funzionalità linguistiche e limiti del sistema. Le query sono state categorizzate in base agli aspetti tecnici che intendono testare.

#### 1. Test del filtro `lowercase` – Case-insensitivity

**Query**: `MOZZARELLA`

**Obiettivo**: Verificare che la ricerca sia case-insensitive grazie al filtro `lowercase`, che normalizza tutto il testo in minuscolo durante indicizzazione e query.

**Comportamento atteso**: La query deve recuperare documenti contenenti "mozzarella", "Mozzarella", "MOZZARELLA" o qualsiasi altra variante di capitalizzazione.

**Risultato**: Confermata l'insensibilità al case; tutti i documenti contenenti il termine, indipendentemente dalla forma originale, vengono correttamente recuperati.

---

#### 2. Test del filtro `italian_elision` – Rimozione elisioni

**Query**: `content: l'origano`

**Obiettivo**: Testare la capacità del filtro `italian_elision` di rimuovere gli articoli elisi tipici della lingua italiana (es. *l'*, *dell'*, *dall'*).

**Comportamento atteso**: Il termine viene normalizzato a "origano", consentendo il match con documenti che contengono sia "l'origano" che "origano".

**Risultato**: Il sistema recupera correttamente entrambe le forme, confermando l'efficacia del filtro nella gestione delle elisioni.

---

#### 3. Test del filtro `italian_stop` – Rimozione stopword

**Query**: `"della panna"`

**Obiettivo**: Verificare che le stopword italiane (come "della", "il", "di") vengano rimosse durante l'analisi, focalizzando la ricerca sui termini semanticamente rilevanti.

**Comportamento atteso**: La stopword "della" viene ignorata; la query cerca effettivamente solo "panna".

**Risultato**: I documenti recuperati contengono il termine "panna" anche in assenza della preposizione articolata "della", confermando il corretto funzionamento del filtro di stopword.

---

#### 4. Test dello stemmer – Normalizzazione plurali

**Query**: `content: mirtilli`

**Obiettivo**: Verificare che lo stemmer Snowball riduca correttamente i plurali alla forma base (*mirtillo/mirtilli → mirtill*).

**Comportamento atteso**: La query deve recuperare documenti contenenti sia "mirtillo" che "mirtilli".

**Risultato**: Entrambe le forme vengono correttamente unificate dalla radice comune, dimostrando l'efficacia dello stemming nella gestione delle variazioni numerali.

---

#### 5. Test dello stemmer – Variazioni di genere

**Query**: `content: fritto`

**Obiettivo**: Testare se lo stemmer gestisce le variazioni di genere e numero (fritto/fritta/fritti/fritte).

**Comportamento atteso**: Tutte le forme devono essere ricondotte alla stessa radice (*fritt*), consentendo match reciproci.

**Risultato**: Il sistema recupera documenti contenenti qualsiasi variazione morfologica del termine, confermando la capacità dello stemmer di normalizzare genere e numero.

---

#### 6. Test dello stemmer – Preservazione distinzioni semantiche

**Query**: `title: pomodoro` vs `title: pomodorino`

**Obiettivo**: Verificare che lo stemmer **leggero** (`light_italian`) preservi le distinzioni semantiche tra termini morfologicamente simili ma concettualmente diversi.

**Comportamento atteso**: "pomodoro" e "pomodorino" devono **non** essere trattati come sinonimi, evitando l'over-stemming.

**Risultato**: Le due query restituiscono insiemi di risultati **distinti**, confermando che lo stemmer mantiene le sfumature semantiche rilevanti nel dominio culinario. Questo comportamento previene falsi positivi che degraderebbero la qualità della ricerca.

---

#### 7. Test di ricerca field-specific – Campo `title`

**Query**: `title: arancini`

**Obiettivo**: Verificare la capacità di eseguire ricerche mirate su un singolo campo indicizzato (in questo caso il titolo della ricetta).

**Comportamento atteso**: Vengono recuperati solo i documenti il cui **titolo** contiene il termine "arancini", ignorando eventuali occorrenze nel campo `content`.

**Risultato**: La ricerca field-specific funziona correttamente, restituendo esclusivamente le ricette con "arancini" nel titolo.

---

#### 8. Test di ricerca field-specific – Campo `content`

**Query**: `content: carbonara`

**Obiettivo**: Testare la ricerca selettiva sul campo `content` (il corpo testuale della preparazione).

**Comportamento atteso**: Solo i documenti che contengono "carbonara" nella descrizione della preparazione vengono restituiti, anche se il termine non compare nel titolo.

**Risultato**: Il sistema isola correttamente i match nel campo specificato, dimostrando la flessibilità della ricerca multi-campo.

---

#### 9. Test delle limitazioni dello stemmer – False positives

**Query**: `ciliegina`

**Obiettivo**: Evidenziare un caso limite in cui lo stemmer può generare **falsi positivi** unificando termini semanticamente diversi ma morfologicamente simili.

**Comportamento atteso**: Lo stemmer potrebbe ridurre sia "ciliegina" che "ciliegino" alla stessa radice (*ciliegin*), causando match indesiderati tra i due termini.

**Risultato**: La query recupera documenti contenenti entrambe le forme, confermando che, in casi specifici, lo stemmer può introdurre ambiguità. Questo rappresenta un compromesso accettabile per mantenere lo stemming leggero e preservare la maggior parte delle distinzioni semantiche.

---

#### 10. Test delle limitazioni dello stemmer – Differenze morfologiche non gestite

**Query**: `content: natalizi` vs `content: natale`

**Obiettivo**: Verificare i limiti dello stemmer nella gestione di variazioni morfologiche derivazionali (aggettivo vs sostantivo).

**Comportamento atteso**: "natalizi" (→ *nataliz*) e "natale" (→ *natal*) producono **stem diversi**, pertanto le due query restituiscono risultati **non sovrapposti**.

**Risultato**: I documenti recuperati dalle due query sono distinti, dimostrando che lo stemmer leggero non unifica forme derivazionali distanti morfologicamente. Questo comportamento è **intenzionale** e coerente con l'approccio conservativo dello stemmer `light_italian`, che privilegia precisione su recall in casi ambigui.

---

### Considerazioni conclusive sulle query di test

I test effettuati dimostrano che l'analyzer italiano implementato in Elasticsearch:
- **Gestisce efficacemente** le variazioni morfologiche di base (plurali, genere, case, elisioni);
- **Rimuove correttamente** le stopword, migliorando la qualità della ricerca;
- **Preserva distinzioni semantiche** importanti grazie allo stemming leggero;
- **Presenta limitazioni note** in casi di derivazioni morfologiche complesse (es. natale/natalizio) e in rari casi di over-stemming (es. ciliegina/ciliegino).

Nel complesso, il trade-off tra normalizzazione morfologica e preservazione semantica risulta **ben bilanciato** per il dominio applicativo delle ricette culinarie, dove la precisione terminologica è spesso più importante del recall assoluto.


