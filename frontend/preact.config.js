export default (config, env, helpers) => {
	if (!env.production) {
		config.devServer.proxy = [
			{
				path: '/rest/**',
				target: 'http://localhost:9000',
				changeOrigin: true,
				changeHost: true
			}
		]
	} else {
		config.output.publicPath = '/frontend/'
	}

	// https://github.com/preactjs/preact-cli/wiki/Config-Recipes#customising-babel-options-using-loader-helpers
	// Preact CLI doesn't support custom babelrc yet, https://github.com/preactjs/preact-cli/pull/1052 was merged but not released
	let { rule } = helpers.getLoadersByName(config, 'babel-loader')[0];
	let babelConfig = rule.options;
	babelConfig.plugins.push(require.resolve('@babel/plugin-proposal-optional-chaining'));
}