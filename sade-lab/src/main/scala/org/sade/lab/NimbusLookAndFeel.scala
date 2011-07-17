package org.sade.lab

import javax.swing.UIManager

trait NimbusLookAndFeel {
  UIManager.getInstalledLookAndFeels.foreach(info => {
    if ("Nimbus" == info.getName) {
      UIManager.setLookAndFeel(info.getClassName);
    }
  })
}