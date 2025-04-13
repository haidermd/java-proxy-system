# Java Proxy System 🛰️

This project is a simple HTTP/S proxy system built in Java with two components:

- **ShipProxy (Client Proxy):** Listens for HTTP/S requests from the browser or tools like `curl` and forwards them.
- **OffshoreProxy (Server Proxy):** Accepts forwarded requests and fetches the actual response from the internet.

Both modules can be built using Maven and deployed via Docker.

---

## 🔧 Technologies Used

- Java 17
- Maven
- Raw Sockets
- Docker
- Optional: Docker Compose

---

## 🗂️ Project Structure

java-proxy-system/ ├── client-proxy/ │ ├── Dockerfile │ ├── src/ │ └── pom.xml ├── server-proxy/ │ ├── Dockerfile │ ├── src/ │ └── pom.xml └── docker-compose.yml



---

## 🚀 Getting Started

### 📦 1. Clone the Repository

```bash
git clone https://github.com/haidermd/java-proxy-system.git
cd java-proxy-system

Build Docker Images
# Build ShipProxy
docker build -t shipproxy ./client-proxy

# Build OffshoreProxy
docker build -t offshoreproxy ./server-proxy

Run Containers Manually

# Start OffshoreProxy (server)
docker run -d -p 9090:9090 offshore-proxy



# Start ShipProxy (client)

docker run -d -p 8080:8080 ship-proxy


Test the Proxy
curl -x http://localhost:8080 http://example.com
curl -x http://localhost:8080 https://www.google.com


Manual Build (Without Docker)
bash
Copy code
# Build both components
cd client-proxy && mvn clean package
cd ../server-proxy && mvn clean package

# Run manually
java -cp target/client-proxy-1.0-SNAPSHOT.jar client.ShipProxy
java -cp target/server-proxy-1.0-SNAPSHOT.jar server.OffshoreProxy
