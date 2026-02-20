# Hundir la Flota - Proyecto PSP

Este proyecto es una implementación moderna del clásico juego **Hundir la Flota (Battleship)**, desarrollada como una aplicación cliente-servidor utilizando **Kotlin Multiplatform (KMP)**. El enfoque principal ha sido crear una arquitectura robusta, una IA táctica desafiante y una interfaz de usuario moderna.

## Características Principales

- **Modos de Juego**:
    - **PvE (Jugador vs IA)**: Enfrentamientos contra una Inteligencia Artificial táctica con tres niveles de dificultad (Fácil, Medio, Difícil).
    - **PvP (Online)**: Matchmaking competitivo a través de salas públicas (Lobby) o privadas (Rooms) mediante códigos.
- **IA Táctica**: Comportamiento avanzado que alterna entre búsqueda aleatoria y asalto dirigido tras detectar un impacto.
- **Interfaz de Usuario (UI)**:
    - Diseño **Modern Warm**: Estética con alto contraste, micro-animaciones y efectos visuales de radar/sonar.
    - Componentes interactivos como el `SonarGrid` y el `BattleLog` para un seguimiento táctico en tiempo real.
- **Persistencia**: Sistema de récords y estadísticas globales (precision, victorias/derrotas, rachas) gestionado por el servidor.
- **Arquitectura**: Desacoplamiento total mediante interfaces, facilitando el testing unitario y de integración.

## Estructura del Proyecto

El proyecto está dividido en tres módulos principales:

- **`common`**: Contiene la lógica compartida, incluyendo el protocolo de comunicación, modelos de datos del juego (`BoardState`, `ShipState`, `Coordinate`) y utilidades de red.
- **`server`**: Motor del juego que gestiona las sesiones, el emparejamiento, la persistencia de datos y la lógica de la IA enemiga.
- **`composeApp`**: Cliente de escritorio desarrollado con **Jetpack Compose Multiplatform**, gestionando el estado de la UI a través de ViewModels reactivos.

## Tecnologías Utilizadas

- **Kotlin 2.1.0** (Multiplatform)
- **Compose Multiplatform** (UI Desktop)
- **Corrutinas y Flow**: Para la gestión de concurrencia y comunicación asíncrona.
- **Kotlinx Serialization**: Protocolo de mensajes basado en JSON.
- **Gradle**: Gestión de dependencias y empaquetado nativo (MSI, EXE, JAR).

## Ejecución y Distribución

### Instalación Rápida (Windows)
Puedes encontrar los instaladores en la carpeta `dist/`:
- `HundirLaFlota-1.0.0.msi` (Instalador estándar)
- `HundirLaFlota-1.0.0.exe` (Ejecutable directo)

### Servidor
El servidor se puede ejecutar de forma independiente usando el JAR sombreado (Fat JAR):
```bash
java -jar dist/HundirLaFlotaServer-1.0.0.jar
```

### Desarrollo
Para compilar y ejecutar desde el código fuente:
```bash
# Ejecutar Servidor
./gradlew :server:run

# Ejecutar Cliente
./gradlew :composeApp:run
```

## Calidad y Testing

El proyecto cuenta con una amplia suite de pruebas para asegurar la estabilidad:
- **Unit Tests**: Lógica de tableros, configuración dinámica y protocolos en `common`.
- **Server Tests**: Validación de sesiones de juego e inteligencia artificial.
- **UI Tests**: Testeo de ViewModels y flujo de estados de navegación.

Para ejecutar todos los tests:
```bash
./gradlew test
```

## Documentación

Todo el código está documentado en **español** utilizando **KDoc**. Encontrarás explicaciones detalladas sobre el funcionamiento de los algoritmos de combate, la gestión de turnos y la tematización del sistema de diseño.

---
**Desarrollado por**: Lois Figueira
