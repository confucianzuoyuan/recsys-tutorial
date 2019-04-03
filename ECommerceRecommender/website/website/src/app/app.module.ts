import { NgModule }      from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule }   from '@angular/forms';
import { HttpModule }    from '@angular/http';

import { AppRoutingModule } from './app-routing.module';

import { AppComponent }         from './app.component';
import { HomeComponent } from './home/home.component';
import { ThumbnailComponent } from './thumbnail/thumbnail.component';
import { MdetailComponent } from './mdetail/mdetail.component';
import { LoginComponent } from './login/login.component';
import { StarComponent } from './star/star.component';
import { RegisterComponent } from './register/register.component';
import { LoginService } from "./services/login.service";
import { HttpClientModule } from "@angular/common/http";
import { RouterModule } from "@angular/router";
import { ExploreComponent } from './explore/explore.component';

@NgModule({
  imports: [
    BrowserModule,
    FormsModule,
    HttpModule,
    AppRoutingModule,
    HttpClientModule,
    FormsModule,
    RouterModule
  ],
  declarations: [
    AppComponent,
    HomeComponent,
    ThumbnailComponent,
    MdetailComponent,
    LoginComponent,
    StarComponent,
    RegisterComponent,
    ExploreComponent,
  ],
  providers: [
    LoginService
  ],
  bootstrap: [ AppComponent ]
})
export class AppModule {
}
