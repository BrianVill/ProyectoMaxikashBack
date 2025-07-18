# 🛠️ Maxikash Backend System

Bienvenido al backend del sistema desarrollado para el proyecto Maxikash, una solución integral construida completamente con Java y Spring Boot, orientada al procesamiento masivo, la gestión compleja de datos y el despliegue en la nube. Este backend fue diseñado, desarrollado y mantenido de forma autónoma como parte de un proyecto real y en producción, donde se aplicaron las mejores prácticas de ingeniería de software, escalabilidad y seguridad.

## 🚀 Tecnologías y Herramientas Utilizadas

- **Lenguaje principal:** Java 17
- **Framework:** Spring Boot
- **Seguridad:** Spring Security + JWT
- **Base de datos:** MySQL (usando AWS RDS y Google Cloud SQL)
- **Cloud:** Google Cloud Run, Cloud Build, Cloud SQL, Cloud Tasks
- **Procesamiento de datos:** Apache POI para lectura de Excel
- **Integraciones:** API de Google Maps para geolocalización
- **Control de versiones:** Git + GitHub
- **Otros:** Railway, Notion, Postman

## 🧠 Funcionalidades Clave

- Diseño y modelado de base de datos relacional con múltiples tablas y relaciones complejas
- Validaciones personalizadas a nivel de entidad y servicio
- Automatización de procesos: carga masiva desde archivos Excel con lógica jerárquica entre entidades
- Procesamiento de múltiples archivos y entrada de datos de manera asincrónica usando Cloud Tasks
- Integración con Google Maps API para obtener latitud y longitud automáticamente desde direcciones
- Implementación de seguridad robusta con Spring Security y autenticación basada en JWT
- División modular del backend en múltiples microservicios Spring Boot (3 proyectos principales con entre 3-6 tablas cada uno)
- Despliegue completo en Google Cloud Platform

## 📂 Estructura del Proyecto

El backend está compuesto por múltiples servicios organizados por dominio funcional, incluyendo:

- Gestión de usuarios y asignaciones
- Variables dinámicas de crédito por día
- Procesos de bonificación y evaluación condicional
- Control de estados de automatización
- Módulo de autenticación y roles

## 🧪 Cómo levantar el proyecto localmente

1. Clonar este repositorio y los servicios relacionados
2. Configurar las variables de entorno o `application.yml`/`.properties` con los valores necesarios (ver archivo de ejemplo)
3. Ejecutar `mvn spring-boot:run` desde cada servicio
4. Consumir los endpoints protegidos con JWT mediante Postman o frontend

⚠️ Las credenciales reales, tokens y secretos fueron removidos por seguridad.

## 🙋‍♂️ Desarrollado por

**Brian Villalva**  
Backend Developer  
📧 villalvab2021@gmail.com  
🔗 [LinkedIn](https://linkedin.com/in/brian-villalva-76b822238)  
💻 [GitHub](https://github.com/BrianVill)  

---
Si querés ver cómo luce un backend desarrollado 100% desde cero en un entorno real de producción, este proyecto es un reflejo completo de ello.
