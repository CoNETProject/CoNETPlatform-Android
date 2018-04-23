"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const http = require("http");
const versions_server = http.createServer((request, response) => {
    response.end('Versions: ' + JSON.stringify(process.versions));
});
versions_server.listen(3000);
console.log('The node project has started.');
