//这是应用的主要入口点。 使用JIT compiler编译器编译本应用，并启动应用的根模块AppModule，使其运行在浏览器中。 你还可以使用AOT compiler编译器，而不用修改任何代码 —— 只要给ng build 或 ng serve 传入 --aot 参数就可以了。
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import {enableProdMode} from '@angular/core';
import { AppModule } from './app/app.module';

enableProdMode();
platformBrowserDynamic().bootstrapModule(AppModule);
