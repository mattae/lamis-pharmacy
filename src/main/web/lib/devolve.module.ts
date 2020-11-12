import {NgModule} from '@angular/core';
import {DevolveDetailsComponent} from './components/devolve.details.component';
import {DevolveEditComponent} from './components/devolve.edit.component';
import {CoreModule} from '@alfresco/adf-core';
import {CommonModule} from '@angular/common';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {CustomFormsModule} from 'ng2-validation';
import {MatDateFormatModule} from '@lamis/web-core';
import {
    MatButtonModule,
    MatCardModule,
    MatDatepickerModule,
    MatInputModule,
    MatSelectModule
} from '@angular/material';
import {DevolveResolve, ROUTES} from './services/devolve.route';
import {RouterModule} from '@angular/router';
import {CovalentCommonModule, CovalentDialogsModule} from '@covalent/core';
import {EndDevolveComponent} from './components/end.devolve.component';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        RouterModule.forChild(ROUTES),
        ReactiveFormsModule,
        CustomFormsModule,
        CoreModule,
        MatDatepickerModule,
        MatDateFormatModule,
        MatInputModule,
        MatButtonModule,
        MatCardModule,
        MatSelectModule,
        CovalentCommonModule,
        CovalentDialogsModule
    ],
    declarations: [
        DevolveDetailsComponent,
        DevolveEditComponent,
        EndDevolveComponent
    ],
    providers: [
        DevolveResolve
    ]
})
export class DevolveModule {

}
