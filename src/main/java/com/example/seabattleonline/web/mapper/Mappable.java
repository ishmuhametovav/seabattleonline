package com.example.seabattleonline.web.mapper;

public interface Mappable<D, E> {
    D toDto(E e);
    E toEntity(D d);
}
