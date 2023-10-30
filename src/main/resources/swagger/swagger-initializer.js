window.onload = function() {
  //<editor-fold desc="Changeable Configuration Block">

  // the following lines will be replaced by docker/configurator, when it runs in a docker-container
  window.ui = SwaggerUIBundle({
    url: "/api-docs/swagger.json",
    dom_id: '#swagger-ui',
    deepLinking: true,
    presets: [
      SwaggerUIBundle.presets.apis,
      SwaggerUIStandalonePreset
    ],
    plugins: [
      SwaggerUIBundle.plugins.DownloadUrl
    ],
    layout: "StandaloneLayout"
  })
  // End Swagger UI call region

  ui.initOAuth({
    clientId: "swagger-ui",
    realm: "service",
    appName: "swagger-ui",
    scopeSeparator: " ",
    persistAuthorization: true,
    additionalQueryStringParams: {"nonce": "132456"}
  })
  //</editor-fold>
};
