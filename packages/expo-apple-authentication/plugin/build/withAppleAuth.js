"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const config_plugins_1 = require("@expo/config-plugins");
const withAppleAuthIOS_1 = require("./withAppleAuthIOS");
const pkg = require('expo-apple-authentication/package.json');
const withAppleAuth = (config) => {
    config = withAppleAuthIOS_1.withAppleAuthIOS(config);
    return config;
};
exports.default = config_plugins_1.createRunOncePlugin(withAppleAuth, pkg.name, pkg.version);
