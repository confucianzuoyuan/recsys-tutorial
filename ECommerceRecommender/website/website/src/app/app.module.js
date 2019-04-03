"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
Object.defineProperty(exports, "__esModule", { value: true });
var core_1 = require("@angular/core");
var platform_browser_1 = require("@angular/platform-browser");
var forms_1 = require("@angular/forms");
var http_1 = require("@angular/http");
var app_routing_module_1 = require("./app-routing.module");
var app_component_1 = require("./app.component");
var home_component_1 = require("./home/home.component");
var thumbnail_component_1 = require("./thumbnail/thumbnail.component");
var mdetail_component_1 = require("./mdetail/mdetail.component");
var login_component_1 = require("./login/login.component");
var tags_component_1 = require("./tags/tags.component");
var star_component_1 = require("./star/star.component");
var register_component_1 = require("./register/register.component");
var login_service_1 = require("./services/login.service");
var http_2 = require("@angular/common/http");
var router_1 = require("@angular/router");
var explore_component_1 = require("./explore/explore.component");
var AppModule = (function () {
    function AppModule() {
    }
    AppModule = __decorate([
        core_1.NgModule({
            imports: [
                platform_browser_1.BrowserModule,
                forms_1.FormsModule,
                http_1.HttpModule,
                app_routing_module_1.AppRoutingModule,
                http_2.HttpClientModule,
                forms_1.FormsModule,
                router_1.RouterModule
            ],
            declarations: [
                app_component_1.AppComponent,
                home_component_1.HomeComponent,
                thumbnail_component_1.ThumbnailComponent,
                mdetail_component_1.MdetailComponent,
                login_component_1.LoginComponent,
                tags_component_1.TagsComponent,
                star_component_1.StarComponent,
                register_component_1.RegisterComponent,
                explore_component_1.ExploreComponent,
            ],
            providers: [
                login_service_1.LoginService
            ],
            bootstrap: [app_component_1.AppComponent]
        })
    ], AppModule);
    return AppModule;
}());
exports.AppModule = AppModule;
//# sourceMappingURL=app.module.js.map