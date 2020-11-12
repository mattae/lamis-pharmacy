import {NgModule} from '@angular/core';
import {PharmacyWidget} from './components/pharmacy.widget';
import {CommonModule} from '@angular/common';
import {CoreModule} from '@alfresco/adf-core';
import {MaterialModule} from './material.module';

@NgModule({
    imports: [
        CommonModule,
        MaterialModule,
        CoreModule
    ],
    declarations: [
        PharmacyWidget
    ],
    entryComponents: [
        PharmacyWidget
    ],
    exports: [
        PharmacyWidget
    ],
    providers: []
})
export class PharmacyWidgetModule {

}
