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
}