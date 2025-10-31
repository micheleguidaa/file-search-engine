# Motore di Ricerca Ricette con Elasticsearch

![Python](https://img.shields.io/badge/Python-3.x-3776AB?style=for-the-badge&logo=python&logoColor=white)
![Elasticsearch](https://img.shields.io/badge/Elasticsearch-9.x-005571?style=for-the-badge&logo=elasticsearch&logoColor=white)
![Pandas](https://img.shields.io/badge/Pandas-150458?style=for-the-badge&logo=pandas&logoColor=white)
![Jupyter](https://img.shields.io/badge/Jupyter-F37626?style=for-the-badge&logo=jupyter&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)

## Descrizione

Questo progetto indicizza una collezione di ricette italiane (file `.txt`) in **Elasticsearch**,  
consentendo la ricerca full-text in **italiano** tra titoli e contenuti delle ricette.

Fornisce un esempio pratico e compatto di come integrare Python con Elasticsearch  
per attività di Information Retrieval.

---

## Caratteristiche

- Crea (e resetta) automaticamente l'indice `index_recipes`  
- Utilizza l'*analizzatore italiano* integrato per una corretta tokenizzazione e stemming del testo  
- Esegue l'indicizzazione bulk efficiente dei file `.txt`  
- Supporta query di tipo `match` e `match_phrase`  
- Misura il tempo di indicizzazione  
- Accetta query interattive dall'utente tramite console  

---

## Requisiti

Installa le dipendenze necessarie:

```bash
pip install -r requirements.txt
```

È inoltre necessaria un'istanza di Elasticsearch in esecuzione (ad esempio, tramite Docker):

```bash
docker compose -f docker/elasticsearch/docker-compose.yml up -d
```

---

## Struttura del Progetto

```
file-search-engine/
├── converter.ipynb          # Notebook per convertire CSV in file di testo
├── elasticsearch.ipynb      # Notebook per indicizzazione e ricerca
├── requirements.txt         # Dipendenze Python
├── dataset/
│   └── recipe.csv          # Dataset originale delle ricette
├── files/                  # File di testo delle ricette (generati)
└── docker/
    └── elasticsearch/      # Configurazione Docker per Elasticsearch
        └── docker-compose.yml
```

---

## Utilizzo

### 1. Avvia Elasticsearch

```bash
docker compose -f docker/elasticsearch/docker-compose.yml up -d
```

### 2. Converti il Dataset

Esegui il notebook `converter.ipynb` per generare i file `.txt` dalla CSV:

```python
# Converte dataset/recipe.csv in singoli file .txt nella cartella files/
```

### 3. Indicizza le Ricette

Esegui il notebook `elasticsearch.ipynb` per:
- Creare l'indice Elasticsearch
- Indicizzare tutte le ricette
- Effettuare ricerche interattive

---

## Esempio di Ricerca

Dopo l'indicizzazione, puoi cercare ricette con query in linguaggio naturale:

```
Cerca: pasta al forno
Cerca: tiramisù con mascarpone
Cerca: dolci natalizi
```

Il sistema restituirà le ricette più pertinenti.

---

## Tecnologie Utilizzate

- **Python 3.x**
- **Elasticsearch 9.x**
- **Pandas** - Manipolazione dati
- **Jupyter Notebook** - Ambiente di sviluppo interattivo
- **Docker** - Containerizzazione di Elasticsearch

---

## Note

- L'analizzatore italiano gestisce correttamente stemming e stop words
- L'indicizzazione bulk migliora le performance per grandi dataset
- L'indice viene ricreato ad ogni esecuzione per garantire consistenza
