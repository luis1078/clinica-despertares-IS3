# Capturas para el README principal

El `README.md` de la raíz referencia dos imágenes que van en esta carpeta.
Mientras no existan, GitHub las muestra como ícono roto — no es un error,
solo faltan por generarse a mano (una captura de pantalla no se puede
automatizar desde esta sesión).

Con la app corriendo en `http://localhost:8080` (`docker compose up -d --build`):

1. **`dashboard-cajero.png`** — inicia sesión con un usuario de rol `CAJERO`
   y captura el panel principal (`/`). Es el que más gráficos muestra: citas
   de los últimos 7 días, citas por estado, ingresos y comprobantes por estado.

2. **`historial-clinico.png`** — con un usuario `ENFERMERA` o `MEDICO`, entra a
   `/pacientes`, busca a **María González (DNI 71234561)** y haz clic en
   "Ver historial". Ese paciente ya tiene historia médica, diagnósticos,
   tratamiento, citas y pagos de ejemplo, así que la línea de tiempo se ve
   completa.

Guarda ambos archivos con esos nombres exactos en esta carpeta y las
imágenes del README quedan enlazadas sin tocar nada más.
