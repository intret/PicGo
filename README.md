# About [![Build Status](https://travis-ci.org/intret/PicGo.svg?branch=master)](https://travis-ci.org/intret/PicGo)

**PicGo**, an android image viewer&managment app built by Kotlin, has these features:

- **👍🏻👍🏻👍🏻 T9 Keypad Filter**: quick T9-Keypad image folder name filtering of image folders from directories `<ExternalStorage>/DCIM` and `<ExternalStorage>/Pictures`.
- **🈯️Drag select** : Press image in image list to entering selection mode, drag to select more images.
- **🔜Move files** : quick move-file-to dialog for moving files to target directory.
- **🌁Image and video** : Supports viewing `gif/jpeg/png/webp` images, and roughly supports viewing video files, likes `."mp4","mov","mpg","mpeg","rmvb","avi"`.
- **🈯️Drag close** : drag-down close image viewer page.
- **🍟Share files** : Share image file to third-party applications whose can accecp the image file.

# Download Apk 

download the latest version from [PicGo GitHub Releases](https://github.com/intret/PicGo/releases)  page

# Architecture

## Design

Google Material Design

## Develop

### Commit message conventions

[Karma - Git Commit Msg](http://karma-runner.github.io/2.0/dev/git-commit-msg.html)

### Application architecture

MVP design pattern with:

- [Kotlin language](https://kotlinlang.org/)
- [Dagger](https://github.com/google/dagger) 2.11+ : used new injection api
- [RxJava2](https://github.com/ReactiveX/RxJava)
- [EventBus](http://greenrobot.org/eventbus/)
- [ButterKnife](https://github.com/JakeWharton/butterknife)
- RxBinding

see more in https://github.com/intret/PicGo/blob/master/app/build.gradle

## Distribute

- [Travis CI](https://travis-ci.org/), see ci configuration file : [.travis.yml](https://github.com/intret/PicGo/blob/master/.travis.yml)，also [ci shell scripts](https://github.com/intret/PicGo/tree/master/tools/ci)
- Git change log generator : ***unfinished***

# App Feature Screenshots

Below listing the screen shot of PicGo.

## Feature - T9 Keypad filter

| ヽ｀ヽ、ヽ｀ヽ｀、ヽ｀｀、ヽ、｀｀、 ｀、ヽ｀｀、 、ヽ ｀ ヽ｀、｀、、ヽヽ｀ヽ、｀｀、 、ヽ｀、ヽ｀｀、ヽ｀ヽ｀、、ヽ ｀ヽ｀｀、ヽ｀｀、ヽ｀｀ヽヽ｀ヽ、ヽ｀ヽ｀、ヽ｀｀、ヽ、｀｀、 ｀、ヽ｀｀ | ヽ｀ヽ、ヽ｀ヽ｀、ヽ｀｀、ヽ、｀｀、 ｀、ヽ｀｀、 、ヽ ｀ ヽ｀、｀、、ヽヽ｀ヽ、｀｀、 、ヽ｀、ヽ｀｀、ヽ｀ヽ｀、、ヽ ｀ヽ｀｀、ヽ｀｀、ヽ｀｀ヽヽ｀ヽ、ヽ｀ヽ｀、ヽ｀｀、ヽ、｀｀、 ｀、ヽ｀｀ |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| input "2886" for "Cuto"                                      | ![picgo-t9-kepad-english](docs/screenshots/jpg/picgo-t9-keypad-cuto.jpg) |
| input "733" for "Reddit"                                     | ![picgo-t9-kepad-reddit](docs/screenshots/jpg/picgo-t9-keypad-english.jpg) |
| input "93" for "文"                                          | ![picgo-t9-kepad-wz](docs/screenshots/jpg/picgo-t9-keypad-wz.jpg) |

## Feature - Image File Name Conflict Dectection

| ヽ｀ヽ、ヽ｀ヽ｀、ヽ｀｀、ヽ、｀｀、 ｀、ヽ｀｀、 、ヽ ｀ ヽ｀、｀、、ヽヽ｀ヽ、｀｀、 、ヽ｀、ヽ｀｀、ヽ｀ヽ｀、、ヽ ｀ヽ｀｀、ヽ｀｀、ヽ｀｀ヽヽ｀ヽ、ヽ｀ヽ｀、ヽ｀｀、ヽ、｀｀、 ｀、ヽ｀｀ | ヽ｀ヽ、ヽ｀ヽ｀、ヽ｀｀、ヽ、｀｀、 ｀、ヽ｀｀、 、ヽ ｀ ヽ｀、｀、、ヽヽ｀ヽ、｀｀、 、ヽ｀、ヽ｀｀、ヽ｀ヽ｀、、ヽ ｀ヽ｀｀、ヽ｀｀、ヽ｀｀ヽヽ｀ヽ、ヽ｀ヽ｀、ヽ｀｀、ヽ、｀｀、 ｀、ヽ｀｀ |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| Name conflict detection                                      | ![picgo-detect-conflict](docs/screenshots/jpg/picgo-detect-conflict.jpg) |


## Feature - Image Viewer

| ヽ｀ヽ、ヽ｀ヽ｀、ヽ｀｀、ヽ、｀｀、 ｀、ヽ｀｀、 、ヽ ｀ ヽ｀、｀、、ヽヽ｀ヽ、｀｀、 、ヽ｀、ヽ｀｀、ヽ｀ヽ｀、、ヽ ｀ヽ｀｀、ヽ｀｀、ヽ｀｀ヽヽ｀ヽ、ヽ｀ヽ｀、ヽ｀｀、ヽ、｀｀、 ｀、ヽ｀｀ | ヽ｀ヽ、ヽ｀ヽ｀、ヽ｀｀、ヽ、｀｀、 ｀、ヽ｀｀、 、ヽ ｀ ヽ｀、｀、、ヽヽ｀ヽ、｀｀、 、ヽ｀、ヽ｀｀、ヽ｀ヽ｀、、ヽ ｀ヽ｀｀、ヽ｀｀、ヽ｀｀ヽヽ｀ヽ、ヽ｀ヽ｀、ヽ｀｀、ヽ、｀｀、 ｀、ヽ｀｀ |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| Image viewer                                      | ![picgo-image-viewer](docs/screenshots/jpg/picgo-image-viewer.jpg) |
| Image Information                                    | ![picgo-image-details](docs/screenshots/jpg/picgo-image-details.jpg) |
| Drag-down-close                                          | ![picgo-image-viewer-drag-down-close](docs/screenshots/jpg/picgo-image-viewer-drag-down-close.jpg) |

## Feature - Quick Move To

| ヽ｀ヽ、ヽ｀ヽ｀、ヽ｀｀、ヽ、｀｀、 ｀、ヽ｀｀、 、ヽ ｀ ヽ｀、｀、、ヽヽ｀ヽ、｀｀、 、ヽ｀、ヽ｀｀、ヽ｀ヽ｀、、ヽ ｀ヽ｀｀、ヽ｀｀、ヽ｀｀ヽヽ｀ヽ、ヽ｀ヽ｀、ヽ｀｀、ヽ、｀｀、 ｀、ヽ｀｀ | ヽ｀ヽ、ヽ｀ヽ｀、ヽ｀｀、ヽ、｀｀、 ｀、ヽ｀｀、 、ヽ ｀ ヽ｀、｀、、ヽヽ｀ヽ、｀｀、 、ヽ｀、ヽ｀｀、ヽ｀ヽ｀、、ヽ ｀ヽ｀｀、ヽ｀｀、ヽ｀｀ヽヽ｀ヽ、ヽ｀ヽ｀、ヽ｀｀、ヽ、｀｀、 ｀、ヽ｀｀ |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| Move image file                                      | ![picgo-move-image-file](docs/screenshots/jpg/picgo-move-image-file.jpg) |


## Feature - MISC

| ヽ｀ヽ、ヽ｀ヽ｀、ヽ｀｀、ヽ、｀｀、 ｀、ヽ｀｀、 、ヽ ｀ ヽ｀、｀、、ヽヽ｀ヽ、｀｀、 、ヽ｀、ヽ｀｀、ヽ｀ヽ｀、、ヽ ｀ヽ｀｀、ヽ｀｀、ヽ｀｀ヽヽ｀ヽ、ヽ｀ヽ｀、ヽ｀｀、ヽ、｀｀、 ｀、ヽ｀｀ | ヽ｀ヽ、ヽ｀ヽ｀、ヽ｀｀、ヽ、｀｀、 ｀、ヽ｀｀、 、ヽ ｀ ヽ｀、｀、、ヽヽ｀ヽ、｀｀、 、ヽ｀、ヽ｀｀、ヽ｀ヽ｀、、ヽ ｀ヽ｀｀、ヽ｀｀、ヽ｀｀ヽヽ｀ヽ、ヽ｀ヽ｀、ヽ｀｀、ヽ、｀｀、 ｀、ヽ｀｀ |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| Open Recents                                     | ![picgo-recents](docs/screenshots/jpg/picgo-recents.jpg) |
| View Mode                                    | ![picgo-view-mode](docs/screenshots/jpg/picgo-view-mode.jpg) |
| Setting                                          | ![picgo-setting](docs/screenshots/jpg/picgo-setting.jpg) |
| Folder excluding setting                                          | ![picgo-exclude-file](docs/screenshots/jpg/picgo-exclude-file.jpg) |

# License

Apache license 2.0