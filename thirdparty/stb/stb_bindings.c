//
// Created by Matej on 22.10.2018.
//

#include "stb_bindings.h"

#define STB_IMAGE_IMPLEMENTATION

#include "stb_image.h"

unsigned char *loadImageFromFile(FILE *f, int *x, int *y, int *channels_in_file, int desired_channels) {
    return stbi_load_from_file(f, x, y, channels_in_file, desired_channels);
}

unsigned char *loadImage(const char *filename, int *x, int *y, int *channels_in_file, int desired_channels) {
    return stbi_load(filename, x, y, channels_in_file, desired_channels);
}

unsigned short *loadImageFromFile16(FILE *f, int *x, int *y, int *channels_in_file, int desired_channels) {
    return stbi_load_from_file_16(f, x, y, channels_in_file, desired_channels);
}

unsigned short *loadImage16(const char *filename, int *x, int *y, int *channels_in_file, int desired_channels) {
    return stbi_load_16(filename, x, y, channels_in_file, desired_channels);
}

float *loadImageFromFileFloat(FILE *f, int *x, int *y, int *channels_in_file, int desired_channels) {
    return stbi_loadf_from_file(f, x, y, channels_in_file, desired_channels);
}

float *loadImageFloat(const char *filename, int *x, int *y, int *channels_in_file, int desired_channels) {
    return stbi_loadf(filename, x, y, channels_in_file, desired_channels);
}

void freeImage(void *retval_from_stbi_load) {
    stbi_image_free(retval_from_stbi_load);
}

void setFlipVerticallyOnLoad(int flag_true_if_should_flip) {
    stbi_set_flip_vertically_on_load(flag_true_if_should_flip);
}