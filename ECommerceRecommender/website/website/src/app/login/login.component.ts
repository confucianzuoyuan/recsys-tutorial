import { Component, OnInit } from '@angular/core';
import {LoginService} from "../services/login.service";
import {User} from "../model/user";

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {

  loginService:LoginService;
  user: User = new User;

  constructor(loginService:LoginService) {
    this.loginService = loginService;
  }

  ngOnInit() {
  }

  login():void{
    this.loginService.login(this.user);
  }
}
