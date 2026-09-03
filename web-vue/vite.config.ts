import path from 'node:path'
import { ConfigEnv, defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import { createHtmlPlugin } from 'vite-plugin-html'
import { visualizer } from 'rollup-plugin-visualizer'
//自动导入vue中hook reactive ref等
import AutoImport from 'unplugin-auto-import/vite'
//自动导入ui-组件 比如说ant-design-vue  element-plus等
import Components from 'unplugin-vue-components/vite'
//ant-design-vue
import { NaiveUiResolver } from 'unplugin-vue-components/resolvers'
import postcss from 'postcss'
// https://vitejs.dev/config/
export default defineConfig(({ mode }: ConfigEnv) => {
  // 加载环境配置
  const env: Record<string, string> = loadEnv(mode, __dirname, 'VOYAGER1')
  const { VOYAGER1_PROXY_HOST: HOST = '', VOYAGER1_BASE_URL = '', VOYAGER1_PORT = '' }: Record<string, string> = env
  console.log(env, `当前为${mode}环境`)

  return {
    envPrefix: 'VOYAGER1_', // 可在项目中通过import.meta.env.VOYAGER1_xxx获取环境变量

    resolve: {
      alias: {
        '@/': `${path.resolve(__dirname, 'src')}/`
      },

      // 忽略后缀名的配置选项
      extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue']
    },
    build: {
      sourcemap: mode !== 'production', // 非生产环境都生成sourcemap
      outDir: '../modules/server/src/main/resources/dist',
      rollupOptions: {
        output: {
          // 用于从入口点创建的块的打包输出格式[name]表示文件名,[hash]表示该文件内容hash值
          entryFileNames: 'assets/js/[name].[hash].js', // 用于命名代码拆分时创建的共享块的输出命名
          chunkFileNames: 'assets/js/[name].[hash].js', // 用于输出静态资源的命名，[ext]表示文件扩展名
          assetFileNames: 'assets/[ext]/[name].[hash].[ext]'
        }
      },
      //打包前清空文件，默认true
      emptyOutDir: true,
      modulePreload: { polyfill: true },
      polyfillModulePreload: true,
      manifest: false
    },
    server: {
      port: Number(VOYAGER1_PORT),
      host: '0.0.0.0',
      proxy: {
        // http
        '/api': {
          target: HOST.includes('http') ? HOST : `http://${HOST}`,
          changeOrigin: true,
          ws: true,
          rewrite: (path) => path.replace(/^\/api/, ''),
          timeout: 10 * 60 * 1000
        }
      }
    },
    plugins: [
      vue(),
      vueJsx(),
      AutoImport({
        //安装两行后你会发现在组件中不用再导入ref，reactive等
        imports: ['vue', 'vue-router', 'pinia'],
        dts: 'src/d.ts/auto-import.d.ts',
        eslintrc: {
          enabled: true,
          filepath: '.eslintrc-auto-import.json',
          globalsPropValue: true
        },

        //naive-ui
        resolvers: [NaiveUiResolver()]
      }),
      AutoImport({
        dirs: ['src/d.ts/global'],
        dts: 'src/d.ts/auto-global-import.d.ts',
        eslintrc: {
          enabled: true,
          filepath: '.eslintrc-global-import.json',
          globalsPropValue: true
        }
      }),
      Components({
        dts: 'src/d.ts/components.d.ts',
        //naive-ui
        resolvers: [NaiveUiResolver()]
      }),
      createHtmlPlugin({
        minify: true,
        inject: {
          data: {
            title: env.VOYAGER1_APP_TITLE,
            base_url: env.VOYAGER1_BASE_URL,
            build: new Date().getTime(),
            env: process.env.NODE_ENV,
            buildVersion: process.env.npm_package_version
          }
        }
      }),
      visualizer({
        emitFile: false,
        // file: 'states.html',
        open: true
      }),
      {
        name: 'vite-plugin-skip-empty-css',
        async transform(code, id) {
          if (/(\.css|\.scss|\.less)$/.test(id)) {
            if (code === '') return ''
            const { root } = await postcss([
              {
                postcssPlugin: 'check-empty-or-comments-only'
              }
            ]).process(code, { from: id })
            if (
              root.nodes.length === 0 ||
              root.nodes.every((node) => {
                return node.type === 'comment'
              })
            ) {
              return ''
            }
            return code
          }
          return code
        }
      }
    ]
  }
})
