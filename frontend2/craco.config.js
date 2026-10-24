const { ModuleFederationPlugin } = require("webpack").container;

module.exports = {
  devServer: (devServerConfig) => {
    devServerConfig.headers = { "Access-Control-Allow-Origin": "*" };
    return devServerConfig;
  },
  webpack: {
    configure: (config) => {
      config.output.publicPath = "auto";
      config.optimization.runtimeChunk = false;
      config.plugins.push(
        new ModuleFederationPlugin({
          name: "frontend2",
          filename: "remoteEntry.js",
          exposes: { "./App": "./src/App" },
          shared: {
            react: { singleton: true, eager: true, requiredVersion: false },
            "react-dom": { singleton: true, eager: true, requiredVersion: false },
          },
        })
      );
      return config;
    },
  },
};
