/**
 * Envoltorio delgado sobre Chart.js para el panel principal. Cada gráfico se
 * describe como un objeto simple ({id, tipo, etiquetas, valores, colores}) y
 * este módulo se encarga de crearlo, y de recrearlo con los colores correctos
 * cuando el usuario cambia de tema (evento "tema-cambiado", disparado por
 * fragments/header.html) — Chart.js no relee variables CSS por su cuenta.
 *
 * `colores` puede ser un arreglo fijo o una función que devuelve uno: usar una
 * función es lo que permite releer las variables --azul/--verde/etc. en cada
 * recreación en vez de quedarse con el valor del primer render.
 *
 * Con Turbo (ver fragments/header.html), este script se re-ejecuta completo
 * cada vez que se visita "/" — es justamente lo que permite recrear los
 * gráficos en cada visita sin depender de DOMContentLoaded (que en una
 * navegación de Turbo no vuelve a dispararse). Pero "document" y "window" no
 * se destruyen entre visitas, así que los listeners globales se registran una
 * sola vez por sesión (bandera en window) y siempre llaman a través de
 * window.ClinicaCharts, que apunta a la generación más reciente del módulo:
 * "tema-cambiado" para recrear los gráficos con los colores del tema nuevo, y
 * "turbo:before-cache" para destruirlos antes de salir del panel (si no,
 * quedan instancias de Chart.js apuntando a canvases que Turbo ya quitó).
 */
(function () {
    'use strict';

    var instancias = [];

    function color(nombreVariable) {
        return getComputedStyle(document.documentElement).getPropertyValue(nombreVariable).trim();
    }

    function opcionesBase(esDona) {
        var texto = color('--texto-suave');
        var borde = color('--borde');
        return {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: esDona,
                    position: 'bottom',
                    labels: { color: texto, boxWidth: 12, padding: 12, font: { size: 11 } }
                }
            },
            scales: esDona ? {} : {
                x: { ticks: { color: texto, font: { size: 11 } }, grid: { display: false } },
                y: { beginAtZero: true, ticks: { color: texto, precision: 0 }, grid: { color: borde } }
            }
        };
    }

    function crear(config) {
        var lienzo = document.getElementById(config.id);
        if (!lienzo || !config.etiquetas || !config.etiquetas.length || typeof window.Chart === 'undefined') {
            return;
        }

        var esDona = config.tipo === 'doughnut';
        var coloresResueltos = typeof config.colores === 'function' ? config.colores() : config.colores;

        instancias.push(new Chart(lienzo, {
            type: config.tipo,
            data: {
                labels: config.etiquetas,
                datasets: [{
                    data: config.valores,
                    backgroundColor: coloresResueltos,
                    borderRadius: esDona ? 0 : 6,
                    borderWidth: esDona ? 2 : 0,
                    borderColor: esDona ? color('--blanco') : undefined,
                    maxBarThickness: 34
                }]
            },
            options: opcionesBase(esDona)
        }));
    }

    function destruirTodo() {
        instancias.forEach(function (chart) { chart.destroy(); });
        instancias = [];
    }

    function iniciar(configuraciones) {
        destruirTodo();
        window.ClinicaCharts.ultimaConfiguracion = configuraciones;
        configuraciones.forEach(crear);
    }

    window.ClinicaCharts = { iniciar: iniciar, color: color, destruirTodo: destruirTodo, ultimaConfiguracion: [] };

    if (!window.__clinicaChartsListenersGlobales) {
        window.__clinicaChartsListenersGlobales = true;

        document.addEventListener('tema-cambiado', function () {
            var config = window.ClinicaCharts && window.ClinicaCharts.ultimaConfiguracion;
            if (config && config.length) {
                window.ClinicaCharts.iniciar(config);
            }
        });

        document.addEventListener('turbo:before-cache', function () {
            if (window.ClinicaCharts && typeof window.ClinicaCharts.destruirTodo === 'function') {
                window.ClinicaCharts.destruirTodo();
            }
        });
    }
})();
