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
| Tempo totale di indicizzazione  | 5.105 s         |

### Discussione dei risultati

Il tempo totale di **5.1 secondi** evidenzia l’elevata efficienza dell’indicizzazione in modalità *bulk*, che consente di minimizzare il numero di richieste HTTP verso il cluster Elasticsearch e di sfruttare in modo ottimale le risorse disponibili.  

Tale valore può tuttavia variare in funzione:
- delle caratteristiche hardware della macchina ospite (CPU, I/O, memoria);
- dello stato e della configurazione del cluster Elasticsearch;
- della dimensione media dei documenti indicizzati.  

Nel complesso, i risultati confermano la **scalabilità e rapidità del processo di indicizzazione** offerto da Elasticsearch, anche su dataset di migliaia di documenti testuali di dimensione medio-piccola.

---

## Query usate per testare il sistema

Il notebook supporta ricerche su entrambi i campi, con due famiglie di query:
- `match` (ricerca lessicale, con analisi/stemming)
- `match_phrase` (ricerca di frase esatta)

Esempi (ripresi dall’interfaccia test del notebook):
- Ricerca per titolo (match):
  - `title: tiramisu`
- Ricerca per frase esatta nel contenuto (match_phrase):
  - `content: "burro e salvia"`
- Ricerca per contenuto (match):
  - `content: banane`

Caratteristiche aggiuntive:
- Restituzione fino a 10.000 risultati per query (`size: 10000`).
- Campo target esplicito (`title` o `content`), coerente con l’uso di analyzer italiano.

---

## Uso di Kibana (UI)

Oltre ai test via notebook, è stato utilizzato Kibana per interrogare l’indice tramite interfaccia grafica. Le attività svolte includono:
- Creazione dell’index pattern per `index_recipes`.
- Esecuzione di query di ricerca in Discover/Dev Tools (esempi equivalenti alle query sopra, usando `match` e `match_phrase`).
- Verifica qualitativa dei risultati e del contributo dello stemming italiano nei match recuperati.

L’utilizzo della UI facilita l’ispezione dei documenti indicizzati (campi `title` e `content`) e l’affinamento iterativo delle query.

---

## Riferimenti tecnici principali

- Indice: `index_recipes`
- Mapping: campi `title` e `content` di tipo `text` con `analyzer: italian` e `search_analyzer: italian`
- Indicizzazione: bulk con `helpers.bulk`
- Dataset → File: generazione `.txt` per ricetta da `converter.ipynb`
- Ambiente: Docker Compose (single‑node Elasticsearch)

---

## Come riprodurre le misure (facoltativo)

Senza eseguire nulla qui, i passi per ottenere i numeri da inserire nelle sezioni precedenti sono:
1) Generare i file `.txt` a partire dal CSV eseguendo le celle di `converter.ipynb`.
2) Avviare Elasticsearch con il docker compose fornito.
3) Eseguire le celle di `elasticsearch.ipynb` per creare l’indice, indicizzare in bulk e misurare i tempi (la cella stampa il numero di documenti indicizzati e i secondi impiegati).
4) Testare sia dal notebook sia da Kibana (UI) con le query di esempio.

Inserire in questa relazione i valori osservati al punto 3.

---

## Possibili estensioni

- Sinonomi e stoplist personalizzate per il dominio culinario (es. equivalenze tra termini regionali/varianti ortografiche).
- Evidenziazione (highlighting) dei termini di ricerca nei risultati.
- Aggiunta di campi strutturati (ingredienti, tempi, difficoltà) e query combinate (full‑text + filtri).
- Benchmark comparativo tra analyzer (italian vs standard, vs custom pipeline).
