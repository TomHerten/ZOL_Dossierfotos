// Package name.
package com.zol_dossierfotos;

/** Creates a category object **/
class Category {

    private String key;
    private String description;

    Category(String key, String description) {
        this.key = key;
        this.description = description;
    }


    String getKey() {
        return key;
    }

    String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Category){
            Category c = (Category) obj;
            if(c.getDescription().equals(description) && c.getKey().equals(key)) return true;
        }
        return false;
    }

    public int hashCode(){
        return 0;
    }

}
