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
