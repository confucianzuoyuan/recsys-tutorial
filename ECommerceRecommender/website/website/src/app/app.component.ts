import { Component } from '@angular/core';
import {Router} from "@angular/router";
import {LoginService} from "./services/login.service";

@Component({
  selector: 'product-app',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {

  login: LoginService;
  query: String = "";

  constructor(public loginService: LoginService, public router: Router) {
    this.login = loginService;
  }

  isLogin(): boolean {
    return this.loginService.isLogin();
  }

  logout(): void {
    this.loginService.logout();
  }

  enterPress(event:any): void {
    if(event.keyCode == 13){
      this.router.navigate(['/explore', { "type": "search", "query": this.query}]);
    }
  }

  search():void{
    this.router.navigate(['/explore', { "type": "search", "query": this.query}]);
  }

}
