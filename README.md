# Elasticsearch Recipe Indexer

This project indexes a collection of recipe `.txt` files into **Elasticsearch**,  
allowing full-text search in **Italian** across recipe titles and contents.

It provides a compact and practical example of how to integrate Python with Elasticsearch  
for Information Retrieval tasks.

---

## Features

- Automatically creates (and resets) the `index_recipes` index  
- Uses the built-in *Italian analyzer* for accurate text tokenization and stemming  
- Performs efficient bulk indexing of `.txt` files  
- Supports both `match` and `match_phrase` queries  
- Measures indexing time  
- Accepts interactive user queries from the console  
- Returns up to 10,000 search results per query  

---

## Requirements

Install the required dependencies:

```bash
pip install -r requirements.txt
```

You also need a running Elasticsearch instance (for example, via Docker):

```bash
docker compose up -d
```