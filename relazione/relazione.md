# Relazione progetto: File Search Engine con Elasticsearch (ricette in italiano)

## URL del progetto

Repository GitHub: https://github.com/micheleguidaa/file-search-engine

---

## Contesto e pipeline

L’obiettivo è indicizzare un corpus di ricette (testo in lingua italiana) in Elasticsearch per consentire ricerche full‑text su titolo e contenuto. Il flusso è composto da due step principali:

1) Estrazione e normalizzazione dati
- Notebook: `converter.ipynb`
- Funzione: legge `dataset/recipe.csv` e genera un file `.txt` per ogni ricetta nella cartella `files/` (uno per riga/ricetta), pulendo i nomi dei file ed eliminando eventuali file preesistenti.

2) Creazione indice e indicizzazione in ES
- Notebook: `elasticsearch.ipynb`
- Funzione: crea/resetta l’indice `index_recipes` con mapping specifico e indicizza in bulk tutti i `.txt` della cartella `files/`.

Ambiente di esecuzione Elasticsearch via Docker (compose in `docker/elasticsearch/docker-compose.yml`):
- Single node (discovery.type=single-node)
- Sicurezza disattivata per semplicità locale (xpack.security.enabled=false)
- Porte esposte: 9200 (HTTP), 9300 (transport)

---

## Analyzer scelti e motivazioni

Nel mapping dell’indice `index_recipes` entrambi i campi sono definiti come `text` con analyzer italiano:
- `title`: `analyzer: "italian"`, `search_analyzer: "italian"`
- `content`: `analyzer: "italian"`, `search_analyzer: "italian"`

Motivazioni della scelta dell’analyzer italiano:
- Stopword italiane: rimozione automatica di articoli/preposizioni e parole funzionali comuni (es. "il", "di", "e"), che non apportano informazione semantica rilevante.
- Stemming/lemmi: riduce le parole alla radice (es. cucinare/cucinato → cucina), migliorando il matching tra varianti morfologiche.
- Gestione accenti e normalizzazione: riduce i falsi negativi dovuti a differenze ortografiche (accenti, maiuscole/minuscole).
- Nessuna customizzazione necessaria: per un primo prototipo, l’analyzer built‑in è un buon compromesso tra qualità e semplicità; eventuali perfezionamenti (sinonimi, stoplist personalizzata) sono demandati a fasi successive.

Questa configurazione rende coerenti l’analisi in indicizzazione e in ricerca (mediante `search_analyzer`), aumentando la probabilità di recuperare documenti pertinenti in lingua italiana.

---

## Numero di file indicizzati e tempi di indicizzazione

Il notebook `elasticsearch.ipynb`:
- crea l’indice `index_recipes` con il mapping di cui sopra;
- esegue l’indicizzazione in bulk di tutti i file `.txt` presenti in `files/` tramite `helpers.bulk`;
- misura il tempo complessivo con una semplice cronometria (`before = time.time()`, `after = time.time()`).

Risultati (da riportare dalla vostra esecuzione locale):
- File indicizzati: <INSERIRE_NUMERO_FILE>
- Tempo di indicizzazione: <INSERIRE_TEMPO_SECONDI> secondi

Note:
- Il numero dei file corrisponde al numero di ricette presenti nel CSV (una ricetta → un file `.txt`).
- La durata dipende dalla macchina e dallo stato del cluster; l’uso del bulk riduce drasticamente il numero di round‑trip verso Elasticsearch.

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
