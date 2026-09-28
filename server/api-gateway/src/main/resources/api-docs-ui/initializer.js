async function initializeDocs() {
  const response = await fetch('/docs/services');
  if (!response.ok) {
    throw new Error('Не удалось загрузить список сервисов');
  }

  window.ui = SwaggerUIBundle({
    urls: await response.json(),
    'urls.primaryName': 'Bloom Identity',
    dom_id: '#swagger-ui',
    presets: [SwaggerUIBundle.presets.apis, SwaggerUIStandalonePreset],
    layout: 'StandaloneLayout',
    deepLinking: true,
    persistAuthorization: false,
    queryConfigEnabled: false,
    validatorUrl: null,
    responseInterceptor(response) {
      const status = document.querySelector('#docs-status');
      const url = new URL(response.url, window.location.origin);
      if (url.pathname.startsWith('/docs/openapi/')) {
        status.hidden = response.status < 400;
        status.textContent = response.status >= 400
          ? 'Описание выбранного сервиса недоступно. Сервис должен быть запущен и отдавать OpenAPI 3. Можно выбрать другой сервис в списке.'
          : '';
      }
      return response;
    },
    requestInterceptor(request) {
      const url = new URL(request.url, window.location.origin);
      if (url.origin !== window.location.origin
          || !(url.pathname.startsWith('/api/v1/') || url.pathname.startsWith('/docs/'))) {
        throw new Error('Запросы разрешены только через Bloom Gateway');
      }
      return request;
    }
  });
}

initializeDocs().catch(error => {
  document.querySelector('#swagger-ui').textContent = error.message;
});
