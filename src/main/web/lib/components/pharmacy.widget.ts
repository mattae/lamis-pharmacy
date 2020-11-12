import {Component, Input, OnInit} from '@angular/core';
import {PharmacyService} from '../services/pharmacy.service';
import {Pharmacy, RegimenInfo} from '../model/pharmacy.model';
import {CardViewDateItemModel, CardViewFloatItemModel, CardViewItem, CardViewTextItemModel} from '@alfresco/adf-core';

@Component({
    selector: 'pharmacy-widget',
    templateUrl: './pharmacy.widget.html'
})
export class PharmacyWidget implements OnInit {
    @Input()
    patientId: number;
    pharmacy: Pharmacy;
    properties: CardViewItem[] = [];

    constructor(private pharmacyService: PharmacyService) {
    }

    ngOnInit(): void {
        this.pharmacyService.latestVisit(this.patientId).subscribe((res) => {
            this.pharmacy = res;
            this.buildProperties();
        });
    }

    buildProperties() {
        this.properties.push(new CardViewDateItemModel({
            key: 'dv',
            value: this.pharmacy.dateVisit,
            label: 'Last Pharmacy Refill',
            format: 'dd MMM, yyyy'
        }));
        this.properties.push(new CardViewDateItemModel({
            key: 'nv',
            value: this.pharmacy.nextAppointment,
            label: 'Next Pharmacy Refill',
            format: 'dd MMM, yyyy'
        }));
        this.properties.push(new CardViewTextItemModel({
            label: 'MMD Type',
            key: 'fs',
            value: this.pharmacy.mmdType
        }));

        this.pharmacyService.regimenInfo(this.pharmacy.patient.id)
            .subscribe((res: RegimenInfo) => {
                this.properties.push(new CardViewTextItemModel({
                    label: 'Regimen Line',
                    key: 'cs',
                    value: res.regimenType
                }));
                this.properties.push(new CardViewTextItemModel({
                    label: 'Regimen',
                    key: 'ts',
                    value: res.regimen
                }));
            });
    }

}
