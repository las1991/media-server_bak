package com.sengled.media;

import java.util.Collection;
import java.util.Observer;

/**
 * @author las
 * @date 18-9-20
 */
public interface Observable {

    void addObserver(Observer o);

    void deleteObserver(Observer o);

    void deleteObservers();

    int countObservers();

    Collection<Observer> getObservers();

    void notifyObservers();

    void notifyObservers(Object arg);

    boolean hasChanged();

    void clearChanged();

    void setChanged();
}
