Notes
=====

Features:
- See connected repository stats.
- List all blocks in the store.
- Inspect a specific block, showing:
  * Block size
  * Detected multicodec headers
  * For standard headers, display contents:
    - `/text/UTF-8` show paragraph
    - `/markdown` render markdown
    - `/bin/` render hex editor view
    - `/image/jpeg` `/image/png` ... show image
  * For merkledag nodes, pretty-print data structure and links to other nodes.
- Keep track of history of viewed nodes
- Clickable node graph (D3?), rooted at some node and breadth-first searching
  the graph for additional nodes. Links should show names, and visited nodes can
  be highlighted somehow.
- Reverse-lookup for nodes which link to some target node.
- Upload a file to store as a block (with optional multicodec headers)
- Build a new merkledag node with links and some data.

Backend server requirements:
- `GET  /blocks/`            List blocks (with pagination)
- `POST /blocks/`            Store a new raw block
- `HEAD /blocks/:id`         Get block stats and storage metadata
- `GET  /blocks/:id`         Get a block's content
- `POST /nodes/`             Create a node by providing structured data.
- `HEAD /nodes/:id`          Get the storage metadata, encoding, and links of a block without the content
- `GET  /nodes/:id`          Get the links and content of a node
- `GET  /nodes/:id/:path*`   Traverse link paths and return the final node.
